export const AUTH_API_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/$/, "");

export function authApi(path: string): string {
  return `${AUTH_API_URL}/api/auth${path}`;
}
