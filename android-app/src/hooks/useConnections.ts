import { useCallback, useEffect, useState } from "react";
import { loadConnections } from "../lib/store";
import type { Connection } from "@shared/types";

export function useConnections() {
  const [connections, setConnections] = useState<Connection[]>([]);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    const loaded = await loadConnections();
    setConnections(loaded);
    setLoading(false);
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { connections, loading, refresh };
}
