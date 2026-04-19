/**
 * VerifiedAvatar
 * A profile-picture circle with an Instagram-style solid black verified badge
 * overlaid on the bottom-right corner — shown only for Pro / Founder accounts.
 *
 * Props:
 *   username  – used to generate the DiceBear avatar seed
 *   avatarUrl – custom avatar URL (optional; falls back to DiceBear)
 *   plan      – 'Free' | 'Pro' | 'Founder' (case-insensitive)
 *   size      – tailwind size class e.g. 'w-10 h-10' (default)
 *   badgeSize – tailwind size class for the badge icon e.g. 'w-4 h-4' (default)
 *   className – extra classes on the outer wrapper
 */

"use client"

import Image from 'next/image'
import { BadgeCheck } from 'lucide-react'

interface VerifiedAvatarProps {
    username: string
    avatarUrl?: string | null
    plan?: string | null
    size?: string
    badgeSize?: string
    className?: string
    fallback?: React.ReactNode
}

function isVerifiedPlan(plan?: string | null) {
    if (!plan) return false
    const p = plan.toLowerCase()
    return p === 'pro' || p === 'founder' || p === 'premium'
}

export default function VerifiedAvatar({
    username,
    avatarUrl,
    plan,
    size = 'w-10 h-10',
    badgeSize = 'w-4 h-4',
    className = '',
    fallback,
}: VerifiedAvatarProps) {
    const verified = isVerifiedPlan(plan)
    const src = avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(username || 'user')}`

    return (
        <span className={`relative inline-flex shrink-0 ${size} ${className}`}>
            {/* Avatar circle */}
            <span className={`${size} rounded-full bg-surface-2 border border-border overflow-hidden relative flex items-center justify-center`}>
                {fallback && !avatarUrl ? (
                    fallback
                ) : (
                    <Image
                        src={src}
                        alt={username}
                        fill
                        className="object-cover"
                        unoptimized
                    />
                )}
            </span>

            {/* Instagram-style verified badge — bottom-right corner */}
            {verified && (
                <span
                    title={`${plan} Verified`}
                    className="absolute -bottom-0.5 -right-0.5 bg-white rounded-full flex items-center justify-center shadow-sm"
                    style={{ padding: '1px' }}
                >
                    <BadgeCheck className={`${badgeSize} fill-black text-white`} />
                </span>
            )}
        </span>
    )
}
