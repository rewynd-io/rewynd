import { useEffect, useState } from "react";

export function usePages<Res, Cursor>(
  loader: (cursor?: Cursor) => Promise<[Res[], Cursor | undefined]>,
  initial?: Cursor,
  limit?: number,
): [Res[], boolean] {
  const [items, setItems] = useState<Res[]>([]);
  const [cursor, setCursor] = useState(initial);
  const [complete, setComplete] = useState(false);
  useEffect(() => {
    (async () => {
      if (limit && items.length >= limit) {
        setComplete(true);
      } else if (!complete) {
        const [newRes, newCursor] = await loader(cursor);
        setCursor(newCursor);
        setItems([...items, ...newRes].slice(0, limit));
        setComplete(!newCursor);
      }
    })();
  }, [cursor]);

  return [items, complete];
}
