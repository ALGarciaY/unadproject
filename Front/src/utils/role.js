/**
 * Normalizacion de roles.
 *
 * El backend puede devolver el rol como `ADMIN` o como `ROLE_ADMIN`, y con
 * distintas capitalizaciones. Antes cada componente repetia su propia version
 * de esta funcion; ahora hay una sola.
 */
export const ROLE_ADMIN = "ADMIN";
export const ROLE_MODERATOR = "MODERATOR";
export const ROLE_USER = "USER";

export const normalizeRole = (role) =>
  String(role || ROLE_USER)
    .replace(/^ROLE_/i, "")
    .toUpperCase();

export const isAdmin = (role) => normalizeRole(role) === ROLE_ADMIN;

/** Moderadores y administradores comparten los permisos de gestion. */
export const isStaff = (role) => normalizeRole(role) !== ROLE_USER;

const ROLE_LABELS = {
  [ROLE_ADMIN]: "Administrador",
  [ROLE_MODERATOR]: "Moderador",
  [ROLE_USER]: "Colaborador",
};

export const roleLabel = (role) => ROLE_LABELS[normalizeRole(role)] || "Usuario";

const ROLE_TONES = {
  [ROLE_ADMIN]: "brand",
  [ROLE_MODERATOR]: "warning",
  [ROLE_USER]: "neutral",
};

export const roleTone = (role) => ROLE_TONES[normalizeRole(role)] || "neutral";
