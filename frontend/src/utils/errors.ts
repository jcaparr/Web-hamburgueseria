export function isSessionExpired(error: unknown): boolean {
  return (error as { response?: { status?: number } })?.response?.status === 401
}
