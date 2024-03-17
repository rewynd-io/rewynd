import { Configuration, DefaultApi } from "@rewynd.io/rewynd-client-typescript";

export const HttpClient: DefaultApi = new DefaultApi(
  new Configuration({ basePath: "" }),
);

export const cardWidth = "20em";
