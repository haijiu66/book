import request from '../utils/request'

export const authApi = {
    login(data) {
        return request.post('/auth/login', data)
    },
    logout() {
        return request.post('/auth/logout')
    },
    register(data) {
        return request.post('/auth/register', data)
    },
    registerNormalUser(data) {
        return request.post('/auth/register-normal-user', data)
    }
}
