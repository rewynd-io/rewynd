import * as Path from "path";

import * as fs from "fs/promises";
import { OpenAPIV3 } from "openapi-types";
import * as spec from "@rewynd.io/rewynd-spec";
const output = process.argv[2];

const httpMethods = [
  "put",
  "post",
  "get",
  "head",
  "options",
  "delete",
  "connect",
  "trace",
  "patch",
] as const;
const paramRegex = /{([^/]+)}/g;

{
  (async () => {
    if (typeof output != "string") {
      throw "Must have output path as argument.";
    }
    const operations = Object.entries((spec as OpenAPIV3.Document).paths)
      .flatMap(([path, pathObject]) =>
        httpMethods
          .map((method) =>
            pathObject[method] && pathObject[method].operationId
              ? {
                  method: method,
                  path: path,
                  operationId: pathObject[method].operationId,
                  pathParamNames: Array.from(path.matchAll(paramRegex)).map(
                    (it) => it[1],
                  ),
                }
              : undefined,
          )
          .filter((it) => it),
      )
      .reduce((acc, { method, path, operationId, pathParamNames }) => {
        return {
          ...acc,
          [operationId]: {
            method: method,
            path: path,
            pathParamNames: pathParamNames,
            expressPath: pathParamNames.reduce((path, paramName) => {
              return path.replace(`{${paramName}}`, `:${paramName}`);
            }, path),
          },
        };
      }, {});

    const operationIds = Object.keys(operations);

    const operationPaths = Object.fromEntries(
      operationIds.map((it) => [it, operations[it].expressPath]),
    );

    const operationParams = Object.fromEntries(
      operationIds.map((it) => [
        it,
        Object.fromEntries(
          operations[it].pathParamNames.map((paramName) => [paramName, ""]),
        ),
      ]),
    );

    await fs.writeFile(
      Path.resolve(output, "operations.ts"),
      `export type OperationId = ${operationIds
        .map((it) => `"${it}"`)
        .join(" | ")}\n` +
        `export const operationPaths = ${JSON.stringify(
          operationPaths,
        )} as const\n` +
        `export const operationParams = ${JSON.stringify(
          operationParams,
        )} as const\n` +
        `export type OperationParams<T extends OperationId> = typeof operationParams[T]\n` +
        `export const paths = ${JSON.stringify(operations)} as const`,
    );
    await fs.writeFile(
      Path.resolve(output, "spec.ts"),
      `export const spec = ${JSON.stringify(spec)} as const`,
    );
    await fs.appendFile(
      Path.resolve(output, "index.ts"),
      "\n" + 'export * from "./operations"\n' + 'export * from "./spec"\n',
    );
  })();
}
