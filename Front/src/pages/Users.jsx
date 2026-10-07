import { useCallback, useEffect, useState } from "react";
import ConfirmDialog from "../components/ConfirmDialog";
import ResourceTable from "../components/ResourceTable";
import Toast from "../components/Toast";
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Icon,
  Input,
  Modal,
  PageHeader,
  Select,
} from "../components/ui";
import useResourceTable from "../hooks/useResourceTable";
import RoleService from "../services/RoleService";
import UserService from "../services/UserService";
import { getUserName } from "../utils/format";
import { roleLabel, roleTone, normalizeRole } from "../utils/role";

const DOCUMENT_TYPES = [
  { value: "CC", label: "Cédula de Ciudadanía" },
  { value: "TI", label: "Tarjeta de Identidad" },
  { value: "CE", label: "Cédula de Extranjería" },
  { value: "PA", label: "Pasaporte" },
];

const emptyForm = {
  firstName: "",
  lastName: "",
  email: "",
  documentType: "CC",
  documentNumber: "",
  roleId: "",
  status: true,
};

/**
 * El backend puede devolver el rol como objeto, como id numerico o como
 * `roleId`. Se conserva esa tolerancia, comparando ids como texto para que no
 * falle si uno llega como numero y el otro como cadena.
 */
const resolveRoleName = (user, roles) => {
  if (user.role && typeof user.role === "object" && user.role.roleName) {
    return user.role.roleName;
  }

  const rawId = typeof user.role === "number" && user.role !== 0 ? user.role : user.roleId;
  if (rawId === undefined || rawId === null || rawId === "") return "Sin rol";

  return roles.find((role) => String(role.id) === String(rawId))?.roleName || "Sin rol";
};

export default function Users() {
  const load = useCallback(() => UserService.getAll(), []);

  const {
    items,
    pageItems,
    loading,
    error,
    setError,
    page,
    changePage,
    toast,
    showToast,
    dismissToast,
    refresh,
  } = useResourceTable({ load, pageSize: 5, errorMessage: "Error al cargar usuarios" });

  const [roles, setRoles] = useState([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(emptyForm);

  // Los roles solo alimentan el desplegable: si fallan, la tabla sigue sirviendo.
  useEffect(() => {
    const loadRoles = async () => {
      try {
        const data = await RoleService.getAll();
        setRoles(Array.isArray(data) ? data : []);
      } catch (requestError) {
        console.error("Error cargando roles:", requestError);
      }
    };

    loadRoles();
  }, []);

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setError("");
    setIsModalOpen(true);
  };

  const openEdit = (user) => {
    setEditing(user);
    setForm({
      firstName: user.firstName || "",
      lastName: user.lastName || "",
      email: user.email || "",
      documentType: user.documentType || "CC",
      documentNumber: user.documentNumber || "",
      roleId: user.role?.id || user.roleId || "",
      status: user.status === undefined || user.status === null ? true : user.status,
    });
    setError("");
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditing(null);
    setForm(emptyForm);
  };

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((current) => ({ ...current, [name]: type === "checkbox" ? checked : value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      if (editing) {
        await UserService.update(editing.id, form);
        showToast("Usuario actualizado correctamente.");
      } else {
        await UserService.create(form);
        showToast("Usuario creado correctamente.");
      }

      closeModal();
      await refresh();
    } catch (requestError) {
      console.error(requestError);
      const message =
        requestError.response?.data?.error ||
        requestError.response?.data?.message ||
        requestError.message ||
        "Error al guardar el usuario";
      setError(message);
      showToast(`Error al guardar el usuario: ${message}`, "error");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!toDelete?.id) return;

    setSaving(true);

    try {
      await UserService.delete(toDelete.id);
      showToast("Usuario eliminado correctamente.");
      setToDelete(null);
      await refresh();
    } catch (requestError) {
      console.error(requestError);
      showToast(
        `Error al eliminar el usuario: ${
          requestError.response?.data?.error ||
          requestError.response?.data?.message ||
          requestError.message
        }`,
        "error"
      );
    } finally {
      setSaving(false);
    }
  };

  const columns = [
    {
      key: "name",
      label: "Usuario",
      render: (user) => {
        const fullName = getUserName(user);
        return (
          <div className="flex items-center gap-3">
            <Avatar name={fullName} size="sm" />
            <div className="min-w-0">
              <p className="truncate font-semibold text-fg">{fullName}</p>
              <p className="truncate text-xs text-fg-subtle">{user.email || "Sin correo"}</p>
            </div>
          </div>
        );
      },
    },
    {
      key: "document",
      label: "Documento",
      render: (user) => (
        <span className="text-fg-muted">
          {user.documentType ? `${user.documentType} ` : ""}
          {user.documentNumber || "N/A"}
        </span>
      ),
    },
    {
      key: "role",
      label: "Rol",
      render: (user) => {
        const roleName = resolveRoleName(user, roles);
        return <Badge tone={roleTone(normalizeRole(roleName))}>{roleLabel(roleName)}</Badge>;
      },
    },
    {
      key: "status",
      label: "Estado",
      render: (user) => (
        <Badge tone={user.status ? "success" : "neutral"} icon={user.status ? "check_circle" : "block"}>
          {user.status ? "Activo" : "Inactivo"}
        </Badge>
      ),
    },
    {
      key: "actions",
      label: "Acciones",
      align: "right",
      render: (user) => (
        <div className="flex justify-end gap-2">
          <Button
            size="sm"
            variant="outline"
            icon="edit"
            onClick={() => openEdit(user)}
            disabled={saving}
            aria-label={`Editar ${getUserName(user)}`}
          >
            Editar
          </Button>
          <Button
            size="sm"
            variant="dangerSoft"
            icon="delete"
            onClick={() => setToDelete(user)}
            disabled={saving}
            aria-label={`Eliminar ${getUserName(user)}`}
          >
            Eliminar
          </Button>
        </div>
      ),
    },
  ];

  const activeCount = items.filter((user) => user.status).length;

  return (
    <div className="flex flex-col gap-6">
      <Toast message={toast?.message} type={toast?.type} onClose={dismissToast} />

      <PageHeader
        eyebrow="Administración"
        title="Gestión de usuarios"
        description="Administra las cuentas registradas, su rol y su estado de acceso."
        actions={
          <Button icon="person_add" onClick={openCreate} disabled={loading}>
            Agregar usuario
          </Button>
        }
      />

      {error && !isModalOpen && <Alert tone="danger">{error}</Alert>}

      <ResourceTable
        icon="group"
        title="Usuarios registrados"
        subtitle={`${items.length} en total · ${activeCount} ${activeCount === 1 ? "activo" : "activos"}`}
        columns={columns}
        rows={pageItems}
        loading={loading}
        emptyIcon="group_off"
        emptyTitle="No hay usuarios registrados"
        emptyDescription="Crea la primera cuenta para que los usuarios puedan acceder a la plataforma."
        emptyAction={
          <Button icon="person_add" onClick={openCreate}>
            Agregar usuario
          </Button>
        }
        page={page}
        totalItems={items.length}
        pageSize={5}
        onPageChange={changePage}
        itemLabel="usuarios"
      />

      <Modal
        open={isModalOpen}
        onClose={closeModal}
        dismissible={!saving}
        size="lg"
        icon={editing ? "edit" : "person_add"}
        title={editing ? "Editar usuario" : "Crear nuevo usuario"}
        subtitle="El número de documento funciona como credencial de acceso."
        footer={
          <>
            <Button variant="outline" onClick={closeModal} disabled={saving}>
              Cancelar
            </Button>
            <Button type="submit" form="user-form" icon="save" loading={saving}>
              {saving ? "Guardando..." : editing ? "Actualizar usuario" : "Crear usuario"}
            </Button>
          </>
        }
      >
        <form id="user-form" onSubmit={handleSubmit} className="grid gap-5 md:grid-cols-2">
          {error && (
            <div className="md:col-span-2">
              <Alert tone="danger">{error}</Alert>
            </div>
          )}

          <Input
            label="Nombre"
            name="firstName"
            value={form.firstName}
            onChange={handleChange}
            disabled={saving}
            required
          />
          <Input
            label="Apellido"
            name="lastName"
            value={form.lastName}
            onChange={handleChange}
            disabled={saving}
            required
          />

          <Input
            label="Correo electrónico"
            name="email"
            type="email"
            value={form.email}
            onChange={handleChange}
            disabled={saving}
            required
          />

          <Select
            label="Tipo de documento"
            name="documentType"
            value={form.documentType}
            onChange={handleChange}
            disabled={saving}
            required
          >
            {DOCUMENT_TYPES.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </Select>

          <Input
            label="Número de documento"
            name="documentNumber"
            value={form.documentNumber}
            onChange={handleChange}
            disabled={saving}
            hint="Se usa como método de autenticación."
            required
          />

          <Select
            label="Rol"
            name="roleId"
            value={form.roleId}
            onChange={handleChange}
            disabled={saving}
            required
          >
            <option value="">Seleccionar...</option>
            {roles.map((role) => (
              <option key={role.id} value={role.id}>
                {role.roleName || role.name}
              </option>
            ))}
          </Select>

          <div className="md:col-span-2">
            <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-line bg-subtle/60 p-4">
              <input
                type="checkbox"
                name="status"
                checked={Boolean(form.status)}
                onChange={handleChange}
                disabled={saving}
                className="mt-0.5 size-4 shrink-0 rounded border-line-strong text-brand focus:ring-2 focus:ring-brand/30"
              />
              <span className="min-w-0">
                <span className="flex items-center gap-1.5 text-sm font-semibold text-fg">
                  <Icon name="toggle_on" size={16} className="text-fg-subtle" />
                  Usuario activo
                </span>
                <span className="mt-0.5 block text-xs text-fg-muted">
                  Un usuario inactivo conserva sus datos pero no debería poder ingresar.
                </span>
              </span>
            </label>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={Boolean(toDelete)}
        title="Eliminar usuario"
        message={`¿Seguro que quieres eliminar la cuenta de "${getUserName(toDelete)}"?`}
        loading={saving}
        onCancel={() => setToDelete(null)}
        onConfirm={handleDelete}
      />
    </div>
  );
}
