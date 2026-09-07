import { assertEquals, assertRejects } from "jsr:@std/assert@^1";
import {
  createLocalJWKSet,
  exportJWK,
  generateKeyPair,
  type JWK,
  SignJWT,
} from "npm:jose@^6";
import {
  type AppCheckKeys,
  AppCheckRejectedError,
  verifyAppCheckToken,
} from "./appcheck.ts";

const PROJECT_NUMBER: string = "815824425834";
const SUBJECT: string = "1:815824425834:android:abc123";
const ISSUER: string = "https://firebaseappcheck.googleapis.com/" +
  PROJECT_NUMBER;
const AUDIENCE: string = "projects/" + PROJECT_NUMBER;

type SigningPair = { privateKey: CryptoKey; keys: AppCheckKeys };

type TokenOptions = {
  issuer?: string;
  audience?: string;
  subject?: string | null;
  issuedAt?: number;
  expiresAt?: number;
  typ?: string;
};

async function signingPair(): Promise<SigningPair> {
  const pair: { privateKey: CryptoKey; publicKey: CryptoKey } =
    await generateKeyPair("RS256", { extractable: true });
  const jwk: JWK = await exportJWK(pair.publicKey);
  const keys: AppCheckKeys = createLocalJWKSet({
    keys: [{ ...jwk, kid: "test-key", alg: "RS256", use: "sig" }],
  });
  return { privateKey: pair.privateKey, keys };
}

async function signToken(
  privateKey: CryptoKey,
  options: TokenOptions = {},
): Promise<string> {
  const now: number = Math.floor(Date.now() / 1000);
  const subject: string | null = options.subject === undefined
    ? SUBJECT
    : options.subject;
  let token: SignJWT = new SignJWT({})
    .setProtectedHeader({
      alg: "RS256",
      kid: "test-key",
      typ: options.typ ?? "JWT",
    })
    .setIssuer(options.issuer ?? ISSUER)
    .setAudience(options.audience ?? AUDIENCE)
    .setIssuedAt(options.issuedAt ?? now)
    .setExpirationTime(options.expiresAt ?? now + 3600);
  if (subject !== null) {
    token = token.setSubject(subject);
  }
  return await token.sign(privateKey);
}

Deno.test("a valid token resolves to its subject", async () => {
  const pair: SigningPair = await signingPair();
  const token: string = await signToken(pair.privateKey);

  const subject: string = await verifyAppCheckToken(
    token,
    PROJECT_NUMBER,
    pair.keys,
  );

  assertEquals(subject, SUBJECT);
});

Deno.test("a missing token is rejected", async () => {
  const pair: SigningPair = await signingPair();

  await assertRejects(
    () => verifyAppCheckToken(null, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
    "The App Check token is missing.",
  );
  await assertRejects(
    () => verifyAppCheckToken("   ", PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
    "The App Check token is missing.",
  );
});

Deno.test("a token for another audience is rejected", async () => {
  const pair: SigningPair = await signingPair();
  const token: string = await signToken(pair.privateKey, {
    audience: "projects/999999999999",
  });

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
  );
});

Deno.test("a token from another issuer is rejected", async () => {
  const pair: SigningPair = await signingPair();
  const token: string = await signToken(pair.privateKey, {
    issuer: "https://attacker.example.com/" + PROJECT_NUMBER,
  });

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
  );
});

Deno.test("an expired token is rejected", async () => {
  const pair: SigningPair = await signingPair();
  const now: number = Math.floor(Date.now() / 1000);
  const token: string = await signToken(pair.privateKey, {
    issuedAt: now - 7200,
    expiresAt: now - 3600,
  });

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
  );
});

Deno.test("a token signed with HS256 is rejected", async () => {
  const pair: SigningPair = await signingPair();
  const now: number = Math.floor(Date.now() / 1000);
  const token: string = await new SignJWT({})
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuer(ISSUER)
    .setAudience(AUDIENCE)
    .setSubject(SUBJECT)
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(new TextEncoder().encode("shared-secret"));

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
  );
});

Deno.test("a token without a subject is rejected", async () => {
  const pair: SigningPair = await signingPair();
  const token: string = await signToken(pair.privateKey, { subject: null });

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, pair.keys),
    AppCheckRejectedError,
    "The App Check token has no subject.",
  );
});

Deno.test("a token signed by an unknown key is rejected", async () => {
  const trusted: SigningPair = await signingPair();
  const rogue: SigningPair = await signingPair();
  const token: string = await signToken(rogue.privateKey);

  await assertRejects(
    () => verifyAppCheckToken(token, PROJECT_NUMBER, trusted.keys),
    AppCheckRejectedError,
  );
});
