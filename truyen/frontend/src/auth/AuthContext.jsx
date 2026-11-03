import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { auth } from '../api/index.js'
import { refreshSession, setAccessToken, setSessionListener } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [ready, setReady] = useState(false)
  const queryClient = useQueryClient()

  useEffect(() => {
    setSessionListener(setUser)
    // F5 trang: thử khôi phục phiên từ cookie
    refreshSession()
      .catch(() => {})
      .finally(() => setReady(true))
  }, [])

  const login = useCallback(
    async (body) => {
      const result = await auth.login(body)
      setAccessToken(result.accessToken)
      setUser(result.user)
      queryClient.invalidateQueries()
      return result.user
    },
    [queryClient],
  )

  const logout = useCallback(async () => {
    try {
      await auth.logout()
    } finally {
      setAccessToken(null)
      setUser(null)
      queryClient.clear()
    }
  }, [queryClient])

  const value = useMemo(
    () => ({ user, setUser, ready, login, logout, isAdmin: Boolean(user?.roles?.includes('ADMIN')) }),
    [user, ready, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
