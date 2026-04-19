import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { User as FirebaseUser } from 'firebase/auth';

export interface UserProfile {
    id: string;
    firebaseUid: string;
    username: string;
    name: string;
    email: string;
    phone: string | null;
    role: string;
    industry: string | null;
    city: string | null;
    state: string | null;
    bio: string | null;
    avatarUrl: string | null;
    coverUrl?: string | null;
    website?: string | null;
    linkedin?: string | null;
    twitter?: string | null;
    github?: string | null;
    isVerified?: boolean;
    subscription?: string;
    plan: string;
    lat: number | null;
    lng: number | null;
    signalCount: number;
    networkSize: number;
}

interface AuthState {
    user: UserProfile | null;
    firebaseUser: FirebaseUser | null;
    token: string | null;
    isAuthenticated: boolean;
    isLoading: boolean;

    setAuth: (firebaseUser: FirebaseUser, token: string, user: UserProfile) => void;
    clearAuth: () => void;
    setLoading: (isLoading: boolean) => void;
    updateUser: (updates: Partial<UserProfile>) => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            user: null,
            firebaseUser: null,
            token: null,
            isAuthenticated: false,
            isLoading: true,

            setAuth: (firebaseUser, token, user) => set({
                firebaseUser,
                token,
                user,
                isAuthenticated: true,
                isLoading: false
            }),
            
            clearAuth: () => set({
                user: null,
                firebaseUser: null,
                token: null,
                isAuthenticated: false,
                isLoading: false
            }),

            setLoading: (isLoading) => set({ isLoading }),
            
            updateUser: (updates) => set((state) => ({
                user: state.user ? { ...state.user, ...updates } : null
            }))
        }),
        {
            name: 'starto-auth-storage',
            storage: createJSONStorage(() => localStorage),
            partialize: (state) => ({ 
                user: state.user, 
                token: state.token, 
                isAuthenticated: state.isAuthenticated 
            }), // Do not persist firebaseUser object directly
        }
    )
);
