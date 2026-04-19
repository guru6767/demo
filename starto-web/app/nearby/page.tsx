"use client"

import { useState, useEffect, useCallback } from 'react'
import Sidebar from '@/components/feed/Sidebar'
import MobileBottomNav from '@/components/feed/MobileBottomNav'
import { Map as MapIcon, Filter, Navigation, Search, Users, ChevronRight, MapPin, Loader2 } from 'lucide-react'
import Image from 'next/image'
import { useAuthStore } from '@/store/useAuthStore'
import { usersApi, ApiUser } from '@/lib/apiClient'
import CityAutocomplete from '@/components/CityAutocomplete'

export default function NearbyEcosystem() {
    const { user, token } = useAuthStore()
    const [nearbyUsers, setNearbyUsers] = useState<ApiUser[]>([])
    const [loading, setLoading] = useState(true)
    
    // Search states
    const [searchCity, setSearchCity] = useState('')
    const [searchLat, setSearchLat] = useState<number | null>(null)
    const [searchLng, setSearchLng] = useState<number | null>(null)
    const [searchRole, setSearchRole] = useState('')
    const [radius, setRadius] = useState(25)

    const roles = [
        { id: '', label: 'All Roles' },
        { id: 'founder', label: 'Founders' },
        { id: 'mentor', label: 'Mentors' },
        { id: 'investor', label: 'Investors' },
        { id: 'talent', label: 'Talent' }
    ]

    const fetchNearby = useCallback(async () => {
        if (!token || !searchLat || !searchLng) {
            setLoading(false)
            return
        }

        setLoading(true)
        const { data, error } = await usersApi.getNearby({
            lat: searchLat,
            lng: searchLng,
            role: searchRole || undefined,
            radius: radius
        }, token)

        if (data) {
            setNearbyUsers(data)
        }
        setLoading(false)
    }, [token, searchLat, searchLng, searchRole, radius])

    // Initial load from user profile
    useEffect(() => {
        if (user) {
            setSearchCity(user.city || '')
            setSearchLat(user.lat)
            setSearchLng(user.lng)
            // By default, show people of the same role or all roles? 
            // User requested: "once the user created account using some role with location then by default in this section you have to detect the nearby users with that location and the role."
            setSearchRole(user.role)
        }
    }, [user])

    useEffect(() => {
        if (searchLat && searchLng) {
            fetchNearby()
        }
    }, [fetchNearby, searchLat, searchLng, searchRole, radius])

    const handleLocationChange = (name: string, lat?: number, lng?: number) => {
        setSearchCity(name)
        if (lat && lng) {
            setSearchLat(lat)
            setSearchLng(lng)
        }
    }

    return (
        <div className="min-h-screen bg-[#0A0A0B] text-white flex justify-center selection:bg-primary/30">
            <div className="max-w-[1400px] w-full flex flex-col md:flex-row pb-16 md:pb-0">
                <Sidebar />

                <main className="flex-1 flex flex-col min-h-screen border-x border-white/5">
                    <header className="p-8 border-b border-white/5 bg-[#0A0A0B]/80 backdrop-blur-xl sticky top-0 z-50">
                        <div className="flex flex-col lg:flex-row gap-6 justify-between items-start lg:items-center">
                            <div>
                                <h1 className="text-3xl font-display font-medium tracking-tight mb-1">Nearby Ecosystem</h1>
                                <p className="text-xs text-gray-500 uppercase tracking-widest font-bold">
                                    Discover strategic partners within {radius}km
                                </p>
                            </div>
                            
                            <div className="flex flex-wrap items-center gap-3 w-full lg:w-auto">
                                <div className="w-full sm:w-64">
                                    <CityAutocomplete 
                                        value={searchCity} 
                                        onChange={handleLocationChange} 
                                    />
                                </div>
                                <select 
                                    value={searchRole}
                                    onChange={(e) => setSearchRole(e.target.value)}
                                    className="bg-white/5 border border-white/10 rounded-lg px-4 py-3.5 text-sm focus:outline-none focus:border-primary/50 transition-all cursor-pointer"
                                >
                                    {roles.map(r => (
                                        <option key={r.id} value={r.id} className="bg-[#1A1B1E]">{r.label}</option>
                                    ))}
                                </select>
                                <button 
                                    onClick={fetchNearby}
                                    className="bg-primary text-black p-3.5 rounded-lg hover:bg-white transition-all active:scale-95 shadow-lg shadow-primary/20"
                                >
                                    <Search className="w-5 h-5" />
                                </button>
                            </div>
                        </div>
                    </header>

                    <div className="flex-1 overflow-y-auto p-8">
                        {loading ? (
                            <div className="flex flex-col items-center justify-center h-[400px] text-gray-500">
                                <Loader2 className="w-10 h-10 animate-spin mb-4 text-primary" />
                                <p className="text-sm font-medium tracking-widest uppercase">Scanning the ecosystem...</p>
                            </div>
                        ) : nearbyUsers.length > 0 ? (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {nearbyUsers.map(u => (
                                    <div 
                                        key={u.id} 
                                        className="group relative bg-white/5 border border-white/5 rounded-2xl p-6 hover:bg-white/[0.08] hover:border-white/10 transition-all cursor-pointer overflow-hidden"
                                    >
                                        <div className="absolute top-0 right-0 p-4 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <div className="bg-primary/10 p-2 rounded-full border border-primary/20">
                                                <ChevronRight className="w-4 h-4 text-primary" />
                                            </div>
                                        </div>

                                        <div className="flex items-start gap-5 mb-6">
                                            <div className="relative">
                                                <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-primary to-primary/40 p-[1px]">
                                                    <div className="w-full h-full rounded-2xl bg-[#1A1B1E] flex items-center justify-center overflow-hidden">
                                                        <Image 
                                                            src={u.avatarUrl || `https://api.dicebear.com/7.x/avataaars/svg?seed=${u.username}`} 
                                                            alt={u.name} 
                                                            fill 
                                                            className="object-cover"
                                                            unoptimized 
                                                        />
                                                    </div>
                                                </div>
                                                {u.isOnline && (
                                                    <div className="absolute -bottom-1 -right-1 w-4 h-4 bg-accent-green rounded-full border-2 border-[#0A0A0B] shadow-lg shadow-accent-green/20" />
                                                )}
                                            </div>
                                            <div className="flex-1">
                                                <h4 className="font-display text-lg font-medium text-white group-hover:text-primary transition-colors">{u.name}</h4>
                                                <p className="text-xs text-primary font-bold uppercase tracking-wider mb-2">{u.role}</p>
                                                <div className="flex items-center gap-1.5 text-gray-500 text-[10px] font-bold uppercase tracking-widest">
                                                    <MapPin className="w-3 h-3" />
                                                    {u.city || 'Satellite'}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="space-y-4">
                                            <p className="text-sm text-gray-400 line-clamp-2 leading-relaxed h-10">
                                                {u.bio || `Passionate ${u.role} contributing to the ${u.industry || 'technology'} ecosystem.`}
                                            </p>
                                            
                                            <div className="flex items-center justify-between pt-4 border-t border-white/5">
                                                <div className="flex flex-col">
                                                    <span className="text-[10px] text-gray-600 uppercase font-bold tracking-widest">Network</span>
                                                    <span className="text-sm font-display">{u.networkSize || 0} nodes</span>
                                                </div>
                                                <div className="flex flex-col items-end">
                                                    <span className="text-[10px] text-gray-600 uppercase font-bold tracking-widest">Trust Store</span>
                                                    <span className="text-sm font-display">{u.signalCount || 0} signals</span>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="flex flex-col items-center justify-center h-[400px] border-2 border-dashed border-white/5 rounded-[2rem]">
                                <div className="bg-white/5 p-6 rounded-full mb-6 relative">
                                    <MapIcon className="w-12 h-12 text-gray-700" />
                                    <div className="absolute inset-0 bg-primary/5 blur-xl rounded-full" />
                                </div>
                                <h3 className="text-xl font-display mb-2">No nodes discovered</h3>
                                <p className="text-gray-500 text-sm mb-8 max-w-xs text-center">
                                    We couldn't find any {searchRole}s within {radius}km of {searchCity}.
                                </p>
                                <button 
                                    onClick={() => setRadius(r => r + 25)}
                                    className="px-8 py-3 bg-white text-black rounded-full font-bold text-xs uppercase tracking-widest hover:scale-105 transition-all active:scale-95"
                                >
                                    Expand Search to {radius + 25}km
                                </button>
                            </div>
                        )}
                    </div>
                </main>
                <MobileBottomNav />
            </div>
        </div>
    )
}
