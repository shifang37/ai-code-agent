// ========== Common ==========

export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface PageRequest {
  pageNum: number
  pageSize: number
  sortField?: string
  sortOrder?: 'ascend' | 'descend'
}

export interface DeleteRequest {
  id: string
}

export interface PageResult<T> {
  records: T[]
  totalRow: number
  pageSize: number
  pageNumber: number
  totalPage: number
}

// ========== User Entity ==========

export interface User {
  id: string
  userAccount: string
  userPassword: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  editTime: string
  createTime: string
  updateTime: string
  isDelete: number
}

// ========== User VO ==========

export interface UserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  createTime: string
}

export interface LoginUserVO {
  id: string
  userAccount: string
  userName: string
  userAvatar: string
  userProfile: string
  userRole: string
  createTime: string
  updateTime: string
}

// ========== Request DTOs ==========

export interface UserRegisterRequest {
  userAccount: string
  userPassword: string
  checkPassword: string
}

export interface UserLoginRequest {
  userAccount: string
  userPassword: string
}

export interface UserAddRequest {
  userName: string
  userAccount: string
  userAvatar?: string
  userProfile?: string
  userRole: string
}

export interface UserUpdateRequest {
  id: string
  userName: string
  userAvatar?: string
  userProfile?: string
  userRole: string
}

export interface UserQueryRequest extends PageRequest {
  id?: string
  userName?: string
  userAccount?: string
  userProfile?: string
  userRole?: string
}

// ========== Enum ==========

export const UserRoleEnum = {
  USER: 'user',
  ADMIN: 'admin',
} as const

export type UserRole = (typeof UserRoleEnum)[keyof typeof UserRoleEnum]

export const UserRoleText: Record<string, string> = {
  [UserRoleEnum.USER]: '用户',
  [UserRoleEnum.ADMIN]: '管理员',
}
