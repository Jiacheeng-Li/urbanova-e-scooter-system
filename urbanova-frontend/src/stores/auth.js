import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    token: localStorage.getItem('accessToken') || null,
    loading: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    currentUser: (state) => state.user
  },

  actions: {
    async login(credentials) {
      this.loading = true
      try {
        const response = await authApi.login(credentials)
        const { accessToken, user } = response.data.data

        this.token = accessToken
        this.user = user

        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('user', JSON.stringify(user))

        return { success: true, user }
      } catch (error) {
        return {
          success: false,
          message: error.response?.data?.error?.message || 'Login failed'
        }
      } finally {
        this.loading = false
      }
    },

    async register(userData) {
      this.loading = true
      try {
        const response = await authApi.register(userData)
        const { accessToken, user } = response.data.data

        this.token = accessToken
        this.user = user

        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('user', JSON.stringify(user))

        return { success: true, user }
      } catch (error) {
        return {
          success: false,
          message: error.response?.data?.error?.message || 'Registration failed'
        }
      } finally {
        this.loading = false
      }
    },

    async fetchCurrentUser() {
      if (!this.token) return null

      try {
        const response = await authApi.getCurrentUser()
        this.user = response.data.data
        localStorage.setItem('user', JSON.stringify(this.user))
        return this.user
      } catch (error) {
        this.logout()
        return null
      }
    },

    logout() {
      this.token = null
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('user')
    }
  }
})
