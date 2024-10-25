export interface Page<Item, Cursor> {
  values: Item[];
  cursor?: Cursor;
}
