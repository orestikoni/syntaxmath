export const AUTH_API_URL = (
  process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080"
).replace(/\/$/, "");

export function authApi(path: string): string {
  return `${AUTH_API_URL}/api/auth${path}`;
}

export async function authFetch(path: string, init: RequestInit): Promise<Response> {
  const csrfResponse = await fetch(authApi("/csrf"), {
    credentials: "include",
    cache: "no-store",
  });
  if (!csrfResponse.ok) {
    throw new Error("Could not initialize request security.");
  }

  const { token } = (await csrfResponse.json()) as { token: string };
  if (!token) {
    throw new Error("Authentication service returned no CSRF token.");
  }
  const headers = new Headers(init.headers);
  headers.set("X-XSRF-TOKEN", token);

  return fetch(authApi(path), {
    ...init,
    credentials: "include",
    headers,
  });
}
