"use client"

import { useState } from 'react'
import { motion } from 'framer-motion'
import { Camera, Check, User, Loader2, Rocket } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useOnboardingStore } from '@/store/useOnboardingStore'
import { useAuthStore } from '@/store/useAuthStore'
import { usersApi } from '@/lib/apiClient'

export default function OnboardingStep5() {
    const router = useRouter()
    const onboarding = useOnboardingStore()
    const { token, updateUser } = useAuthStore()
    
    const [username, setUsername] = useState(onboarding.username)
    const [bio, setBio] = useState(onboarding.bio)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleEnterStarto = async () => {
        if (!token) {
            setError("Session expired. Please log in again.")
            return
        }

        setLoading(true)
        setError(null)
        
        try {
            const { data, error: apiError } = await usersApi.updateProfile({
                name: onboarding.name || username,
                username: username,
                bio: bio,
                role: onboarding.role,
                industry: onboarding.industry,
                city: onboarding.city,
                lat: onboarding.lat || undefined,
                lng: onboarding.lng || undefined
            }, token)

            if (data) {
                updateUser(data)
                router.push('/feed')
            } else {
                setError(apiError || "Failed to update profile. Please try again.")
            }
        } catch (err) {
            setError("Something went wrong. Check your connection.")
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-[#0A0A0B] flex items-center justify-center p-6 font-sans">
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-[#161618] p-10 rounded-[2rem] border border-white/5 max-w-lg w-full shadow-2xl relative overflow-hidden"
            >
                <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-primary/5 blur-[100px] rounded-full" />

                <div className="relative z-10">
                    <div className="mb-10 text-center">
                        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-[10px] uppercase font-bold tracking-widest mb-4">
                            Step 05 / 05
                        </div>
                        <h2 className="text-3xl font-display text-white mb-3">Set up your identity.</h2>
                        <p className="text-gray-400 text-sm">Final step to enter the Starto ecosystem.</p>
                    </div>

                    <div className="flex flex-col items-center mb-10">
                        <div className="relative group cursor-pointer">
                            <div className="w-28 h-28 bg-white/5 rounded-full flex items-center justify-center border-2 border-dashed border-white/10 group-hover:border-primary/50 transition-all">
                                <User className="w-12 h-12 text-gray-600 group-hover:text-primary transition-colors" />
                            </div>
                            <div className="absolute bottom-1 right-1 bg-primary p-2.5 rounded-full text-black border-4 border-[#161618] shadow-lg hover:scale-110 transition-transform">
                                <Camera className="w-4 h-4" />
                            </div>
                        </div>
                    </div>

                    <div className="space-y-6 mb-10">
                        <div>
                            <label className="text-[10px] uppercase tracking-widest font-bold text-gray-500 mb-2 block">Username</label>
                            <div className="relative group">
                                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-600 font-bold group-focus-within:text-primary transition-colors">@</span>
                                <input
                                    type="text"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    placeholder="your_handle"
                                    className="w-full bg-white/5 p-4 pl-10 rounded-xl outline-none border border-white/5 focus:border-primary/50 focus:bg-white/10 transition-all text-white text-sm"
                                />
                                {username.length > 3 && (
                                    <Check className="absolute right-4 top-1/2 -translate-y-1/2 w-4 h-4 text-accent-green" />
                                )}
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <label className="text-[10px] uppercase tracking-widest font-bold text-gray-500 block">Bio</label>
                                <span className="text-[10px] text-gray-600 font-bold uppercase">{bio.length}/300</span>
                            </div>
                            <textarea
                                maxLength={300}
                                value={bio}
                                onChange={(e) => setBio(e.target.value)}
                                placeholder="Your ecosystem mission..."
                                className="w-full bg-white/5 p-4 rounded-xl outline-none border border-white/5 focus:border-primary/50 focus:bg-white/10 transition-all text-white text-sm h-32 resize-none"
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="mb-6 p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-500 text-xs text-center">
                            {error}
                        </div>
                    )}

                    <button
                        onClick={handleEnterStarto}
                        disabled={!username || loading}
                        className={`w-full py-4 rounded-xl font-bold uppercase tracking-[0.2em] text-xs transition-all flex items-center justify-center gap-3 shadow-xl ${
                            username && !loading
                            ? 'bg-primary text-black hover:scale-[1.02] active:scale-[0.98]' 
                            : 'bg-white/5 text-gray-600 cursor-not-allowed'
                        }`}
                    >
                        {loading ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                        ) : (
                            <>
                                Enter Starto
                                <Rocket className="w-4 h-4" />
                            </>
                        )}
                    </button>
                </div>
            </motion.div>
        </div>
    )
}
