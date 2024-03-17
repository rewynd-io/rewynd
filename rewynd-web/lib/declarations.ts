/* eslint @typescript-eslint/no-namespace: 0 */
/* eslint @typescript-eslint/no-empty-interface: 0 */

import { User as ClientUser } from "@rewynd.io/rewynd-client-typescript";

declare global {
  namespace Express {
    interface User extends ClientUser {}
  }
}

declare global {
  namespace Express {
    interface User extends ClientUser {}
  }
}
