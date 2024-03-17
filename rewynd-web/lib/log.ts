import { CategoryProvider } from "typescript-logging-category-style";
import { LogLevel } from "typescript-logging";

export const WebLog = CategoryProvider.createProvider("RewyndIoLogProvider", {
  level: LogLevel.Info,
})
  .getCategory("rewynd.io")
  .getChildCategory("web");
