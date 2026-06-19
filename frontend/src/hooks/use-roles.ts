import { useAuth } from "react-oidc-context";

/** Reads Keycloak's realm_access.roles claim off the current access token. */
export function useRoles(): string[] {
  const auth = useAuth();
  const claims = auth.user?.profile as { realm_access?: { roles?: string[] } } | undefined;
  return claims?.realm_access?.roles ?? [];
}

export function useHasRole(role: string): boolean {
  return useRoles().includes(role);
}
