"use client"
import { useEffect, ReactNode } from 'react'
import { useAuthStore } from '@/store/useAuthStore'
import { usersApi } from '@/lib/apiClient'
import { auth } from '@/lib/firebase'
import { onAuthStateChanged } from 'firebase/auth'

export default function AuthProvider({ children }: { children: ReactNode }) {
    const { token, isAuthenticated, user, clearAuth, setAuth, setLoading } = useAuthStore()

    // 1. Listen for Firebase Auth changes to sync store
    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, async (firebaseUser) => {
            if (firebaseUser) {
                const token = await firebaseUser.getIdToken()
                // If we have a firebase user but no profile in store, we might need to fetch it
                // But usually setAuth is called during login/register
                // This listener ensures that if the token refreshes, we can update it (optional enhancement)
            } else {
                // If firebase user is gone, clear our store
                if (isAuthenticated) clearAuth()
            }
            setLoading(false)
        })

        return () => unsubscribe()
    }, [isAuthenticated, clearAuth, setAuth, setLoading])

    // 2. Heartbeat every 30 seconds
    useEffect(() => {
        if (!isAuthenticated || !token) return

        const sendHeartbeat = async () => {
            try {
                await usersApi.heartbeat(token)
            } catch (error) {
                console.error('Heartbeat failed', error)
            }
        }

        // Send immediately on mount/auth
        sendHeartbeat()

        const interval = setInterval(sendHeartbeat, 30000)
        return () => clearInterval(interval)
    }, [isAuthenticated, token])

    return <>{children}</>
}
