export type Coordinates = { id: number; x: number; y: number };
export type Discipline = {
  id: number;
  name: string;
  lectureHours: number;
  selfStudyHours: number;
  labsCount: number;
};
export type Location = { id: number; x: number; y: number; z: number };
export type Person = {
  id: number;
  name: string;
  eyeColor: string | null;
  hairColor: string;
  location: Location | null;
  weight: number | null;
  nationality: string | null;
};
export type IdName = { id: number; name: string };
export type LabWork = {
  id: number;
  name: string;
  coordinates: Coordinates;
  creationDate: string;
  description: string | null;
  difficulty: string | null;
  discipline: Discipline;
  minimalPoint: number;
  tunedInWorks: number | null;
  author: Person | null;
  programs: IdName[] | null;
};
export type LabWorkBrief = { id: number; name: string; difficulty: string | null };
export type Page<T> = { items: T[]; total: number; page: number; size: number };
export type Catalogs = {
  coordinates: Coordinates[];
  disciplines: Discipline[];
  persons: Person[];
  locations: Location[];
};
export type DeleteInfo = {
  requiresReplacement: boolean;
  referenceCount: number;
  message: string;
  programs?: IdName[] | null;
};
export type DeleteResult = { deletedId: number; replacementId: number | null };
export type AuthUser = { token?: string | null; username: string; role: string };
export type AverageResult = { average: number | null };
export type UniquePoints = { values: number[] };
export type AddHardestResult = { labWorkIds: number[]; inserted: number };
export type CatalogKind = "coordinates" | "disciplines" | "persons" | "locations";
export type ListQuery = {
  page: number;
  size: number;
  name?: string;
  description?: string;
  difficulty?: string;
  disciplineName?: string;
  authorName?: string;
  sort?: string;
  order?: "asc" | "desc";
};

export type CoordinatesInput = { id?: number; x?: number; y?: number };
export type DisciplineInput = {
  id?: number;
  name?: string;
  lectureHours?: number;
  selfStudyHours?: number;
  labsCount?: number;
};
export type LocationInput = { id?: number; x?: number; y?: number; z?: number };
export type PersonInput = {
  id?: number;
  name?: string;
  eyeColor?: string | null;
  hairColor?: string;
  weight?: number | null;
  nationality?: string | null;
  location?: LocationInput | null;
};
export type LabWorkInput = {
  name: string;
  description?: string | null;
  difficulty?: string | null;
  minimalPoint: number;
  tunedInWorks?: number | null;
  coordinates: CoordinatesInput;
  discipline: DisciplineInput;
  author: PersonInput | null;
};

export const DIFFICULTIES = ["NORMAL", "HARD", "VERY_HARD", "IMPOSSIBLE", "TERRIBLE"] as const;
export const COLORS = ["RED", "BLACK", "YELLOW", "BROWN"] as const;
export const COUNTRIES = ["RUSSIA", "GERMANY", "SPAIN", "INDIA"] as const;

export function dash(value: string | number | null | undefined): string {
  if (value === null || value === undefined || value === "") return "—";
  return String(value);
}

export function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("ru-RU");
}
