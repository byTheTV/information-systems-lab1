import type {
  AddHardestResult,
  AuthUser,
  AverageResult,
  CatalogKind,
  Coordinates,
  DeleteInfo,
  DeleteResult,
  Discipline,
  LabWork,
  LabWorkBrief,
  LabWorkInput,
  ListQuery,
  Location,
  Page,
  Person,
  UniquePoints,
} from "./types";

export class ApiError extends Error {
  status: number;
  fields?: Record<string, string>;
  requiresReplacement?: boolean;

  constructor(status: number, message: string, fields?: Record<string, string>, requiresReplacement?: boolean) {
    super(message);
    this.status = status;
    this.fields = fields;
    this.requiresReplacement = requiresReplacement;
  }
}

type ErrorBody = { message?: string; fields?: Record<string, string>; requiresReplacement?: boolean };

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers);
  const token = localStorage.getItem("token");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (init?.body) headers.set("Content-Type", "application/json");
  const response = await fetch(path, { ...init, headers });
  if (response.status === 204) return undefined as T;
  const text = await response.text();
  const data = text ? (JSON.parse(text) as T & ErrorBody) : null;
  if (!response.ok) {
    const body = data as ErrorBody | null;
    if (response.status === 401 && !path.startsWith("/api/auth/login") && !path.startsWith("/api/auth/register")) {
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      localStorage.removeItem("role");
      if (!window.location.pathname.startsWith("/login")) window.location.assign("/login");
    }
    throw new ApiError(response.status, body?.message || "Запрос отклонён", body?.fields, body?.requiresReplacement);
  }
  return data as T;
}

function query(params: Record<string, string | number | null | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && String(value) !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export const api = {
  login: (username: string, password: string) =>
    request<AuthUser>("/api/auth/login", { method: "POST", body: JSON.stringify({ username, password }) }),
  register: (username: string, password: string) =>
    request<AuthUser>("/api/auth/register", { method: "POST", body: JSON.stringify({ username, password }) }),
  logout: () => request<void>("/api/auth/logout", { method: "POST" }),
  me: () => request<AuthUser>("/api/auth/me"),
  list: (params: ListQuery) => request<Page<LabWork>>(`/api/labworks${query(params)}`),
  briefs: () => request<LabWorkBrief[]>("/api/labworks/brief"),
  get: (id: number) => request<LabWork>(`/api/labworks/${id}`),
  create: (body: LabWorkInput) => request<LabWork>("/api/labworks", { method: "POST", body: JSON.stringify(body) }),
  update: (id: number, body: LabWorkInput) =>
    request<LabWork>(`/api/labworks/${id}`, { method: "PUT", body: JSON.stringify(body) }),
  deleteInfo: (id: number) => request<DeleteInfo>(`/api/labworks/${id}/delete-info`),
  remove: (id: number, replacementId?: number) =>
    request<DeleteResult>(`/api/labworks/${id}${query({ replacementId })}`, { method: "DELETE" }),
  coordinates: () => request<Coordinates[]>("/api/catalog/coordinates"),
  disciplines: () => request<Discipline[]>("/api/catalog/disciplines"),
  persons: () => request<Person[]>("/api/catalog/persons"),
  locations: () => request<Location[]>("/api/catalog/locations"),
  catalogInfo: (kind: CatalogKind, id: number) => request<DeleteInfo>(`/api/catalog/${kind}/${id}/delete-info`),
  catalogRemove: (kind: CatalogKind, id: number, replacementId?: number) =>
    request<DeleteResult>(`/api/catalog/${kind}/${id}${query({ replacementId })}`, { method: "DELETE" }),
  average: () => request<AverageResult>("/api/special/average-minimal-point"),
  byDescription: (substring: string) =>
    request<LabWork[]>(`/api/special/by-description${query({ substring })}`),
  uniquePoints: () => request<UniquePoints>("/api/special/unique-minimal-points"),
  decrease: (labWorkId: number, steps: number) =>
    request<LabWork>("/api/special/decrease-difficulty", {
      method: "POST",
      body: JSON.stringify({ labWorkId, steps }),
    }),
  addHardest: (disciplineId: number) =>
    request<AddHardestResult>("/api/special/add-hardest", {
      method: "POST",
      body: JSON.stringify({ disciplineId }),
    }),
};

export type { ListQuery };
