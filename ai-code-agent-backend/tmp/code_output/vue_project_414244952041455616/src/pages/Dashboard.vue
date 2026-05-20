<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
        <div class="stat-info">
          <span class="stat-value">{{ stat.value }}</span>
          <span class="stat-label">{{ stat.label }}</span>
        </div>
        <div class="stat-change" :class="stat.trend">{{ stat.change }}</div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card chart-card">
        <div class="card-header">
          <h3>本周销售趋势</h3>
          <span class="card-subtitle">近7日销售额（元）</span>
        </div>
        <div class="chart-container">
          <div class="bar-chart">
            <div class="bar-item" v-for="(item, idx) in weeklyData" :key="idx">
              <div class="bar-label">{{ item.label }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: item.percent + '%' }"></div>
              </div>
              <div class="bar-value">¥{{ (item.value / 10000).toFixed(1) }}万</div>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h3>热销商品 Top 5</h3>
        </div>
        <div class="top-products">
          <div class="top-item" v-for="(item, idx) in topProducts" :key="idx">
            <span class="top-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
            <div class="top-info">
              <span class="top-name">{{ item.name }}</span>
              <span class="top-detail">销量 {{ item.sales.toLocaleString() }} | 金额 ¥{{ (item.amount / 10000).toFixed(0) }}万</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h3>最近订单</h3>
        <router-link to="/orders" class="btn btn-sm btn-outline">查看全部</router-link>
      </div>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>订单号</th>
              <th>客户</th>
              <th>金额</th>
              <th>状态</th>
              <th>下单时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in recentOrders" :key="order.id">
              <td>
                <router-link :to="'/orders/' + order.id" class="order-link">{{ order.id }}</router-link>
              </td>
              <td>{{ order.customer }}</td>
              <td>¥{{ order.total.toLocaleString() }}</td>
              <td>
                <span class="status-tag" :class="'status-' + statusMap[order.status]">{{ order.status }}</span>
              </td>
              <td>{{ order.date }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { dashboardStats } from '@/utils/mockData.js'

const stats = [
  { icon: '💰', label: '今日销售额', value: '¥' + (dashboardStats.todaySales).toLocaleString(), change: '+12.5%', trend: 'up', bg: 'linear-gradient(135deg, #667eea, #764ba2)' },
  { icon: '📝', label: '今日订单', value: dashboardStats.todayOrders, change: '+8.3%', trend: 'up', bg: 'linear-gradient(135deg, #f093fb, #f5576c)' },
  { icon: '👤', label: '新增用户', value: dashboardStats.todayUsers, change: '+15.7%', trend: 'up', bg: 'linear-gradient(135deg, #4facfe, #00f2fe)' },
  { icon: '📦', label: '商品总数', value: dashboardStats.totalProducts, change: '+3.2%', trend: 'up', bg: 'linear-gradient(135deg, #43e97b, #38f9d7)' }
]

const weeklyData = dashboardStats.weeklySales.map((v, i) => ({
  label: dashboardStats.weeklyLabels[i],
  value: v,
  percent: Math.round(v / Math.max(...dashboardStats.weeklySales) * 100)
}))

const topProducts = dashboardStats.topProducts
const recentOrders = dashboardStats.recentOrders

const statusMap = {
  '已完成': 'completed',
  '已取消': 'cancelled',
  '待发货': 'pending',
  '已发货': 'shipped',
  '待付款': 'pending'
}
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: #fff;
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}
.stat-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--gray-800);
}
.stat-label {
  font-size: 13px;
  color: var(--gray-500);
  margin-top: 2px;
}
.stat-change {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 600;
}
.stat-change.up {
  background: #d1fae5;
  color: #065f46;
}
.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}
.chart-card .card-header {
  margin-bottom: 0;
  border-bottom: none;
}
.card-subtitle {
  font-size: 12px;
  color: var(--gray-400);
}
.chart-container {
  padding: 10px 0;
}
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 200px;
  padding-top: 20px;
}
.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}
.bar-track {
  width: 36px;
  height: 150px;
  background: var(--gray-100);
  border-radius: 6px;
  position: relative;
  overflow: hidden;
}
.bar-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(to top, var(--primary), var(--primary-light));
  border-radius: 6px 6px 0 0;
  transition: height 0.6s;
}
.bar-label {
  font-size: 12px;
  color: var(--gray-500);
  margin-top: 8px;
}
.bar-value {
  font-size: 11px;
  color: var(--gray-400);
  margin-bottom: 6px;
}
.top-products {
  padding: 4px 0;
}
.top-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--gray-100);
}
.top-item:last-child {
  border-bottom: none;
}
.top-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: var(--gray-200);
  color: var(--gray-600);
  flex-shrink: 0;
}
.rank-1 { background: #f59e0b; color: #fff; }
.rank-2 { background: #9ca3af; color: #fff; }
.rank-3 { background: #d97706; color: #fff; }
.top-info {
  display: flex;
  flex-direction: column;
}
.top-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--gray-800);
}
.top-detail {
  font-size: 12px;
  color: var(--gray-400);
  margin-top: 2px;
}
.order-link {
  color: var(--primary);
  font-weight: 500;
}
.order-link:hover {
  color: var(--primary-dark);
}
@media (max-width: 992px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 576px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
