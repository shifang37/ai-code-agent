import request from '@/request'
import type {
  BaseResponse,
  UserRegisterRequest,
  UserLoginRequest,
  UserAddRequest,
  UserUpdateRequest,
  UserQueryRequest,
  DeleteRequest,
  LoginUserVO,
  UserVO,
  User,
  PageResult,
} from '@/models'

export function userRegister(data: UserRegisterRequest) {
  return request.post<BaseResponse<string>>('/user/register', data)
}

export function userLogin(data: UserLoginRequest) {
  return request.post<BaseResponse<LoginUserVO>>('/user/login', data)
}

export function getLoginUser() {
  return request.get<BaseResponse<LoginUserVO>>('/user/get/login')
}

export function userLogout() {
  return request.post<BaseResponse<boolean>>('/user/logout')
}

export function addUser(data: UserAddRequest) {
  return request.post<BaseResponse<string>>('/user/add', data)
}

export function getUserById(id: string) {
  return request.get<BaseResponse<User>>('/user/get', { params: { id } })
}

export function getUserVOById(id: string) {
  return request.get<BaseResponse<UserVO>>('/user/get/vo', { params: { id } })
}

export function deleteUser(data: DeleteRequest) {
  return request.post<BaseResponse<boolean>>('/user/delete', data)
}

export function updateUser(data: UserUpdateRequest) {
  return request.post<BaseResponse<boolean>>('/user/update', data)
}

export function listUserVOByPage(data: UserQueryRequest) {
  return request.post<BaseResponse<PageResult<UserVO>>>('/user/list/page/vo', data)
}
