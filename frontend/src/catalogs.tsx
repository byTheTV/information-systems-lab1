import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "./api";
import { useLive } from "./live";
import type { Catalogs } from "./types";

export function useCatalogs() {
  const [data, setData] = useState<Catalogs | null>(null);
  const [error, setError] = useState("");
  const reload = useCallback(async () => {
    try {
      const [coordinates, disciplines, persons, locations] = await Promise.all([
        api.coordinates(),
        api.disciplines(),
        api.persons(),
        api.locations(),
      ]);
      setData({ coordinates, disciplines, persons, locations });
      setError("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Не удалось загрузить справочники");
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  useLive(() => {
    void reload();
  });

  return { data, error, reload };
}
