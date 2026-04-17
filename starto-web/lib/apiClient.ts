/**
 * Starto API Client
 * Centralized HTTP client for all backend calls.
 * Uses NEXT_PUBLIC_API_BASE_URL from .env.local (default: http://localhost:8081)
 */

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8081';

// ─── Types matching the Spring Boot backend models ───────────────────────────

export interface ApiSignal {
    id: string;           // UUID
    title: string;
    description: string;
    category: string;
    type: string;         // "need" | "help"
    seeking: string;
    status: string;       // "open" | "closed"
    username: string;
    city: string;
    signalStrength: string;
    viewCount: number;
    responseCount: number;
    offerCount: number;
    createdAt: string;    // ISO datetime
    expiresAt: string;
    userId: string;
    userPlan?: string;
}

export interface ApiUser {
    id: string;
    firebaseUid: string;
    email: string;
    name: string;
    username: string;
    role: string;
    bio: string | null;
    city: string | null;
    state: string | null;
    industry: string | null;
    websiteUrl: string | null;
    linkedinUrl: string | null;
    twitterUrl: string | null;
    githubUrl: string | null;
    plan: string;
    isOnline: boolean;
    lastSeen: string;
    signalCount?: number;
    networkSize?: number;
}

export interface ApiComment {
    id: string;
    content: string;
    username: string;
    createdAt: string;
    replies?: ApiComment[];
}

export interface CreateSignalPayload {
    title: string;
    description: string;
    category: string;
    type: string;
    seeking: string;
    city?: string;
    timelineDays?: number;
    signalStrength?: string;
}

// ─── Core fetch helper ───────────────────────────────────────────────────────

async function apiFetch<T>(
    path: string,
    options: RequestInit = {},
    token?: string | null
): Promise<{ data: T | null; error: string | null; status: number }> {
    const headers: Record<string, string> = {
        'Content-Type': 'application/json',
        ...(options.headers as Record<string, string> || {}),
    };

    if (token) {
        const finalToken = token.startsWith('local-') ? token.replace('local-', 'dev_') : token;
        headers['Authorization'] = `Bearer ${finalToken}`;
    }

    try {
        const res = await fetch(`${BASE_URL}${path}`, {
            cache: 'no-store',
            ...options,
            headers,
        });

        if (res.status === 204) {
            return { data: null, error: null, status: 204 };
        }

        const text = await res.text();
        let data: T | null = null;
        try {
            data = text ? JSON.parse(text) : null;
        } catch {
            data = text as unknown as T;
        }

        if (!res.ok) {
            const errMsg = (data as any)?.error || (data as any)?.message || `HTTP ${res.status}`;
            return { data: null, error: errMsg, status: res.status };
        }

        return { data, error: null, status: res.status };
    } catch (err: any) {
        return { data: null, error: err.message || 'Network error', status: 0 };
    }
}

// ─── Signal API ──────────────────────────────────────────────────────────────

export const signalsApi = {
    /** GET /api/signals — public, no auth required */
    getAll: (params?: { city?: string; seeking?: string; username?: string }) => {
        const qs = params
            ? '?' + Object.entries(params)
                .filter(([, v]) => v != null)
                .map(([k, v]) => `${k}=${encodeURIComponent(v!)}`)
                .join('&')
            : '';
        return apiFetch<ApiSignal[]>(`/api/signals${qs}`);
    },

    /** GET /api/signals/:id — public */
    getById: (id: string) =>
        apiFetch<ApiSignal>(`/api/signals/${id}`),

    /** POST /api/signals — requires Firebase auth token */
    create: (payload: CreateSignalPayload, token: string) =>
        apiFetch<ApiSignal>('/api/signals', {
            method: 'POST',
            body: JSON.stringify(payload),
        }, token),

    /** DELETE /api/signals/:id — requires auth */
    delete: (id: string, token: string) =>
        apiFetch<void>(`/api/signals/${id}`, { method: 'DELETE' }, token),
};

// ─── User API ────────────────────────────────────────────────────────────────

export const usersApi = {
    /** GET /api/users/:username — public */
    getByUsername: (username: string) =>
        apiFetch<ApiUser>(`/api/users/${username}`),

    /** GET /api/auth/me — requires auth */
    getMe: (token: string) =>
        apiFetch<ApiUser>('/api/auth/me', {}, token),

    /** POST /api/auth/register — requires auth (Firebase token in header) */
    register: (payload: Partial<ApiUser>, token: string) =>
        apiFetch<ApiUser>('/api/auth/register', {
            method: 'POST',
            body: JSON.stringify(payload),
        }, token),

    /** PUT /api/users/profile — requires auth */
    updateProfile: (payload: Partial<ApiUser>, token: string) =>
        apiFetch<ApiUser>('/api/users/profile', {
            method: 'PUT',
            body: JSON.stringify(payload),
        }, token),
};

// ─── Comment API ─────────────────────────────────────────────────────────────

export const commentsApi = {
    /** GET /api/signals/:signalId/comments — public */
    getForSignal: (signalId: string) =>
        apiFetch<ApiComment[]>(`/api/signals/${signalId}/comments`),

    /** POST /api/signals/:signalId/comments — requires auth */
    post: (signalId: string, content: string, token: string) =>
        apiFetch<ApiComment>(`/api/signals/${signalId}/comments`, {
            method: 'POST',
            body: JSON.stringify({ content }),
        }, token),
};

// ─── Connection API ──────────────────────────────────────────────────────────
export const connectionsApi = {
    /** POST /api/connections/request */
    sendRequest: (signalId: string, message: string, token: string) =>
        apiFetch<any>('/api/connections/request', {
            method: 'POST',
            body: JSON.stringify({ signalId, message }),
        }, token),

    /** GET /api/connections/pending — incoming for founder */
    getPending: (token: string) =>
        apiFetch<any[]>('/api/connections/pending', {}, token),

    /** GET /api/connections/sent — outgoing for talent */
    getSent: (token: string) =>
        apiFetch<any[]>('/api/connections/sent', {}, token),

    /** GET /api/connections/accepted */
    getAccepted: (token: string) =>
        apiFetch<any[]>('/api/connections/accepted', {}, token),

    /** PUT /api/connections/:id/accept */
    accept: (id: string, token: string) =>
        apiFetch<any>(`/api/connections/${id}/accept`, { method: 'PUT' }, token),

    /** PUT /api/connections/:id/reject */
    reject: (id: string, token: string) =>
        apiFetch<any>(`/api/connections/${id}/reject`, { method: 'PUT' }, token),
};

// ─── Offer API ───────────────────────────────────────────────────────────────
export const offersApi = {
    create: (payload: { signalId: string; organizationName: string; portfolioLink: string; message: string }, token: string) =>
        apiFetch<any>('/api/offers/request', {
            method: 'POST',
            body: JSON.stringify(payload),
        }, token),

    getInbox: (token: string) =>
        apiFetch<any[]>('/api/offers/inbox', {}, token),

    getSent: (token: string) =>
        apiFetch<any[]>('/api/offers/sent', {}, token),

    getWhatsappLink: (id: string, token: string) =>
        apiFetch<{ link: string }>(`/api/offers/${id}/whatsapp`, {}, token),
};

export default apiFetch;
