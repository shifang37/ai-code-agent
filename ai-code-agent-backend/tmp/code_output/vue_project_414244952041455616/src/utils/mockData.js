// 商品数据
export const products = [
  { id: 1, name: '华为Mate 60 Pro', category: '手机数码', price: 6999, stock: 128, sales: 3420, status: '上架', image: 'https://picsum.photos/seed/phone/200/200', created: '2024-01-15' },
  { id: 2, name: 'Apple MacBook Air M3', category: '电脑办公', price: 8999, stock: 56, sales: 1876, status: '上架', image: 'https://picsum.photos/seed/macbook/200/200', created: '2024-02-20' },
  { id: 3, name: '索尼WH-1000XM5 耳机', category: '数码配件', price: 2499, stock: 203, sales: 5632, status: '上架', image: 'https://picsum.photos/seed/headphone/200/200', created: '2024-01-10' },
  { id: 4, name: '小米智能电视S75', category: '家用电器', price: 3999, stock: 42, sales: 892, status: '上架', image: 'https://picsum.photos/seed/tv/200/200', created: '2024-03-05' },
  { id: 5, name: 'Nike Air Force 1 运动鞋', category: '鞋靴箱包', price: 799, stock: 315, sales: 12056, status: '上架', image: 'https://picsum.photos/seed/shoe/200/200', created: '2024-01-20' },
  { id: 6, name: '雅诗兰黛小棕瓶精华', category: '美妆护肤', price: 690, stock: 178, sales: 8920, status: '上架', image: 'https://picsum.photos/seed/skincare/200/200', created: '2024-02-14' },
  { id: 7, name: '戴森V15吸尘器', category: '家用电器', price: 4990, stock: 67, sales: 2341, status: '上架', image: 'https://picsum.photos/seed/dyson/200/200', created: '2024-03-12' },
  { id: 8, name: 'iPad Air 第六代', category: '电脑办公', price: 4799, stock: 94, sales: 4567, status: '上架', image: 'https://picsum.photos/seed/ipad/200/200', created: '2024-02-28' },
  { id: 9, name: '三只松鼠坚果礼盒', category: '食品饮料', price: 128, stock: 856, sales: 23450, status: '上架', image: 'https://picsum.photos/seed/nuts/200/200', created: '2024-01-05' },
  { id: 10, name: '任天堂Switch OLED', category: '游戏娱乐', price: 2599, stock: 33, sales: 6789, status: '下架', image: 'https://picsum.photos/seed/switch/200/200', created: '2024-03-18' },
  { id: 11, name: '飞利浦电动牙刷HX9352', category: '个护健康', price: 399, stock: 245, sales: 15678, status: '上架', image: 'https://picsum.photos/seed/toothbrush/200/200', created: '2024-02-01' },
  { id: 12, name: 'Columbia 冲锋衣', category: '运动户外', price: 1299, stock: 88, sales: 3456, status: '上架', image: 'https://picsum.photos/seed/jacket/200/200', created: '2024-03-22' }
]

// 订单数据
export const orders = [
  { id: 'ORD20240301001', customer: '张伟', phone: '138****1234', total: 8999, items: 1, status: '已完成', address: '北京市朝阳区建国路88号', date: '2024-03-01 10:30', products: [{ name: 'Apple MacBook Air M3', qty: 1, price: 8999, image: 'https://picsum.photos/seed/macbook/80/80' }] },
  { id: 'ORD20240301002', customer: '李娜', phone: '139****5678', total: 3198, items: 2, status: '已发货', address: '上海市浦东新区陆家嘴环路100号', date: '2024-03-01 14:20', products: [{ name: '索尼WH-1000XM5 耳机', qty: 1, price: 2499, image: 'https://picsum.photos/seed/headphone/80/80' }, { name: '飞利浦电动牙刷', qty: 1, price: 699, image: 'https://picsum.photos/seed/toothbrush/80/80' }] },
  { id: 'ORD20240302003', customer: '王强', phone: '136****9012', total: 799, items: 1, status: '待发货', address: '广州市天河区天河路385号', date: '2024-03-02 09:15', products: [{ name: 'Nike Air Force 1 运动鞋', qty: 1, price: 799, image: 'https://picsum.photos/seed/shoe/80/80' }] },
  { id: 'ORD20240302004', customer: '赵敏', phone: '137****3456', total: 1380, items: 2, status: '待付款', address: '深圳市南山区科技园南路1号', date: '2024-03-02 16:45', products: [{ name: '雅诗兰黛小棕瓶精华', qty: 2, price: 690, image: 'https://picsum.photos/seed/skincare/80/80' }] },
  { id: 'ORD20240303005', customer: '刘洋', phone: '158****7890', total: 6999, items: 1, status: '已完成', address: '杭州市西湖区文三路478号', date: '2024-03-03 11:00', products: [{ name: '华为Mate 60 Pro', qty: 1, price: 6999, image: 'https://picsum.photos/seed/phone/80/80' }] },
  { id: 'ORD20240303006', customer: '陈静', phone: '159****2345', total: 3999, items: 1, status: '已发货', address: '成都市高新区天府大道999号', date: '2024-03-03 13:30', products: [{ name: '小米智能电视S75', qty: 1, price: 3999, image: 'https://picsum.photos/seed/tv/80/80' }] },
  { id: 'ORD20240304007', customer: '孙鹏', phone: '186****6789', total: 256, items: 2, status: '已完成', address: '武汉市洪山区珞喻路1037号', date: '2024-03-04 08:20', products: [{ name: '三只松鼠坚果礼盒', qty: 2, price: 128, image: 'https://picsum.photos/seed/nuts/80/80' }] },
  { id: 'ORD20240304008', customer: '周婷', phone: '187****0123', total: 2599, items: 1, status: '待发货', address: '南京市鼓楼区中山北路200号', date: '2024-03-04 15:10', products: [{ name: '任天堂Switch OLED', qty: 1, price: 2599, image: 'https://picsum.photos/seed/switch/80/80' }] },
  { id: 'ORD20240305009', customer: '吴刚', phone: '188****4567', total: 4990, items: 1, status: '已取消', address: '重庆市渝中区解放碑步行街1号', date: '2024-03-05 09:40', products: [{ name: '戴森V15吸尘器', qty: 1, price: 4990, image: 'https://picsum.photos/seed/dyson/80/80' }] },
  { id: 'ORD20240305010', customer: '郑丽', phone: '189****8901', total: 4799, items: 1, status: '已完成', address: '西安市雁塔区长安南路88号', date: '2024-03-05 17:55', products: [{ name: 'iPad Air 第六代', qty: 1, price: 4799, image: 'https://picsum.photos/seed/ipad/80/80' }] }
]

// 用户数据
export const users = [
  { id: 1001, name: '张伟', email: 'zhangwei@example.com', phone: '138****1234', orders: 23, totalSpent: 45680, status: '活跃', registerDate: '2023-06-12', avatar: 'https://picsum.photos/seed/user1/60/60' },
  { id: 1002, name: '李娜', email: 'lina@example.com', phone: '139****5678', orders: 18, totalSpent: 32150, status: '活跃', registerDate: '2023-08-25', avatar: 'https://picsum.photos/seed/user2/60/60' },
  { id: 1003, name: '王强', email: 'wangqiang@example.com', phone: '136****9012', orders: 7, totalSpent: 8920, status: '活跃', registerDate: '2023-11-03', avatar: 'https://picsum.photos/seed/user3/60/60' },
  { id: 1004, name: '赵敏', email: 'zhaomin@example.com', phone: '137****3456', orders: 45, totalSpent: 123400, status: '活跃', registerDate: '2023-03-18', avatar: 'https://picsum.photos/seed/user4/60/60' },
  { id: 1005, name: '刘洋', email: 'liuyang@example.com', phone: '158****7890', orders: 12, totalSpent: 15670, status: '休眠', registerDate: '2024-01-09', avatar: 'https://picsum.photos/seed/user5/60/60' },
  { id: 1006, name: '陈静', email: 'chenjing@example.com', phone: '159****2345', orders: 31, totalSpent: 78900, status: '活跃', registerDate: '2023-05-22', avatar: 'https://picsum.photos/seed/user6/60/60' },
  { id: 1007, name: '孙鹏', email: 'sunpeng@example.com', phone: '186****6789', orders: 5, totalSpent: 3400, status: '休眠', registerDate: '2024-02-14', avatar: 'https://picsum.photos/seed/user7/60/60' },
  { id: 1008, name: '周婷', email: 'zhouting@example.com', phone: '187****0123', orders: 29, totalSpent: 56780, status: '活跃', registerDate: '2023-07-30', avatar: 'https://picsum.photos/seed/user8/60/60' }
]

// 分类数据
export const categories = [
  { id: 1, name: '手机数码', desc: '手机、平板、数码相机等', count: 86, sort: 1, status: '启用', created: '2024-01-01' },
  { id: 2, name: '电脑办公', desc: '笔记本、台式机、办公设备', count: 54, sort: 2, status: '启用', created: '2024-01-01' },
  { id: 3, name: '家用电器', desc: '电视、空调、洗衣机、小家电', count: 112, sort: 3, status: '启用', created: '2024-01-02' },
  { id: 4, name: '鞋靴箱包', desc: '运动鞋、皮鞋、箱包配件', count: 203, sort: 4, status: '启用', created: '2024-01-03' },
  { id: 5, name: '美妆护肤', desc: '化妆品、护肤品、香水', count: 178, sort: 5, status: '启用', created: '2024-01-05' },
  { id: 6, name: '食品饮料', desc: '零食、饮料、生鲜食品', count: 256, sort: 6, status: '启用', created: '2024-01-08' },
  { id: 7, name: '运动户外', desc: '运动器材、户外装备、健身用品', count: 67, sort: 7, status: '禁用', created: '2024-01-10' },
  { id: 8, name: '游戏娱乐', desc: '游戏主机、游戏周边、玩具', count: 43, sort: 8, status: '启用', created: '2024-02-01' }
]

// 仪表盘统计数据
export const dashboardStats = {
  todaySales: 128900,
  todayOrders: 156,
  todayUsers: 23,
  totalProducts: 1256,
  weeklySales: [18900, 23400, 19800, 25600, 28700, 31900, 28900],
  weeklyLabels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
  topProducts: [
    { name: 'Nike Air Force 1', sales: 12056, amount: 9632800 },
    { name: '三只松鼠坚果礼盒', sales: 23450, amount: 3001600 },
    { name: '飞利浦电动牙刷', sales: 15678, amount: 6255522 },
    { name: '雅诗兰黛小棕瓶', sales: 8920, amount: 6154800 },
    { name: '任天堂Switch OLED', sales: 6789, amount: 17646561 }
  ],
  recentOrders: [
    { id: 'ORD20240305010', customer: '郑丽', total: 4799, status: '已完成', date: '2024-03-05 17:55' },
    { id: 'ORD20240305009', customer: '吴刚', total: 4990, status: '已取消', date: '2024-03-05 09:40' },
    { id: 'ORD20240304008', customer: '周婷', total: 2599, status: '待发货', date: '2024-03-04 15:10' },
    { id: 'ORD20240304007', customer: '孙鹏', total: 256, status: '已完成', date: '2024-03-04 08:20' },
    { id: 'ORD20240303006', customer: '陈静', total: 3999, status: '已发货', date: '2024-03-03 13:30' }
  ]
}
