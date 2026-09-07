import {
  createRemoteJWKSet,
  jwtVerify,
  type JWTVerifyGetKey,
} from "npm:jose@^6";

export class AppCheckRejectedError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "AppCheckRejectedError";
  }
}

export type AppCheckKeys = JWTVerifyGetKey;

const APP_CHECK_JWKS_URL: string =
  "https://firebaseappcheck.googleapis.com/v1/jwks";

const appCheckKeys: ReturnType<typeof createRemoteJWKSet> = createRemoteJWKSet(
  new URL(APP_CHECK_JWKS_URL),
);

export async function verifyAppCheckToken(
  token: string | null,
  projectNumber: string,
  keys: AppCheckKeys = appCheckKeys,
): Promise<string> {
  if (token === null || token.trim().length === 0) {
    throw new AppCheckRejectedError("The App Check token is missing.");
  }
  try {
    const verified = await jwtVerify(token, keys, {
      algorithms: ["RS256"],
      typ: "JWT",
      issuer: "https://firebaseappcheck.googleapis.com/" + projectNumber,
      audience: "projects/" + projectNumber,
    });
    const subject: unknown = verified.payload.sub;
    if (typeof subject !== "string" || subject.length === 0) {
      throw new AppCheckRejectedError("The App Check token has no subject.");
    }
    return subject;
  } catch (error: unknown) {
    if (error instanceof AppCheckRejectedError) {
      throw error;
    }
    throw new AppCheckRejectedError("The App Check token is not valid.");
  }
}
