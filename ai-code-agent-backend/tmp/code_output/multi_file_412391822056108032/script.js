// script.js — 商城交互逻辑 (购物车、商品渲染、响应菜单)
document.addEventListener('DOMContentLoaded', function() {
    'use strict';

    // ---------- 商品数据 ----------
    const productsData = [
        {
            id: 1,
            name: '极简设计台灯',
            description: '触控调光 | 护眼柔和 | 适合书桌床头',
            price: 249.00,
            img: 'https://picsum.photos/400/400?random=10'
        },
        {
            id: 2,
            name: '帆布双肩包',
            description: '复古风格 · 大容量 · 防水底衬',
            price: 189.00,
            img: 'https://picsum.photos/400/400?random=20'
        },
        {
            id: 3,
            name: '陶瓷咖啡杯组',
            description: '手工釉色 · 附杯碟 · 两色可选',
            price: 119.00,
            img: 'https://picsum.photos/400/400?random=30'
        },
        {
            id: 4,
            name: '无线蓝牙耳机',
            description: '降噪通话 · 24h续航 · 舒适佩戴',
            price: 399.00,
            img: 'https://picsum.photos/400/400?random=40'
        },
        {
            id: 5,
            name: '香薰蜡烛礼盒',
            description: '大吉岭茶 & 白麝香 · 天然大豆蜡',
            price: 159.00,
            img: 'https://picsum.photos/400/400?random=50'
        },
        {
            id: 6,
            name: '棉麻夏日衬衫',
            description: '轻薄透气 · 宽松版型 · 三色',
            price: 279.00,
            img: 'https://picsum.photos/400/400?random=60'
        }
    ];

    // ---------- 状态 ----------
    let cart = []; // 存储 { product, quantity }

    // DOM 元素
    const productsGrid = document.getElementById('productsGrid');
    const cartItemsContainer = document.getElementById('cartItemsContainer');
    const cartTotalPrice = document.getElementById('cartTotalPrice');
    const cartCountBadge = document.getElementById('cartCountBadge');
    const cartSidebar = document.getElementById('cartSidebar');
    const overlay = document.getElementById('overlay');
    const cartIconWrapper = document.getElementById('cartIconWrapper');
    const closeCartBtn = document.getElementById('closeCartBtn');
    const mobileMenuToggle = document.getElementById('mobileMenuToggle');
    const navList = document.querySelector('.nav-list');

    // ---------- 渲染商品 ----------
    function renderProducts() {
        if (!productsGrid) return;
        productsGrid.innerHTML = productsData.map(product => {
            return `
                <div class="product-card" data-id="${product.id}">
                    <img src="${product.img}" alt="${product.name}" loading="lazy">
                    <h3>${product.name}</h3>
                    <p class="product-description">${product.description}</p>
                    <div class="price">¥${product.price.toFixed(2)}</div>
                    <button class="btn-add-cart" data-id="${product.id}">加入购物车</button>
                </div>
            `;
        }).join('');
    }

    // ---------- 购物车核心操作 ----------
    function addToCart(productId) {
        const product = productsData.find(p => p.id === productId);
        if (!product) return;

        const existingItem = cart.find(item => item.product.id === productId);
        if (existingItem) {
            existingItem.quantity += 1;
        } else {
            cart.push({ product, quantity: 1 });
        }
        updateCartUI();
        openCartSidebar(); // 添加后自动打开购物车
    }

    function removeItem(productId) {
        cart = cart.filter(item => item.product.id !== productId);
        updateCartUI();
    }

    function changeQuantity(productId, delta) {
        const item = cart.find(item => item.product.id === productId);
        if (!item) return;
        const newQty = item.quantity + delta;
        if (newQty <= 0) {
            removeItem(productId);
        } else {
            item.quantity = newQty;
            updateCartUI();
        }
    }

    function getTotalItems() {
        return cart.reduce((acc, item) => acc + item.quantity, 0);
    }

    function getTotalPrice() {
        return cart.reduce((acc, item) => acc + item.product.price * item.quantity, 0);
    }

    // ---------- 更新购物车 UI (侧栏 + 徽章) ----------
    function updateCartUI() {
        // 更新徽章
        const totalItems = getTotalItems();
        cartCountBadge.textContent = totalItems;

        // 更新侧栏列表
        if (!cartItemsContainer) return;
        if (cart.length === 0) {
            cartItemsContainer.innerHTML = `<p class="cart-empty-message">🛒 购物车还是空的，快去逛逛吧！</p>`;
            cartTotalPrice.textContent = `¥0.00`;
            return;
        }

        let html = '';
        cart.forEach(item => {
            const p = item.product;
            html += `
                <div class="cart-item" data-id="${p.id}">
                    <img src="${p.img}" alt="${p.name}" loading="lazy">
                    <div class="cart-item-info">
                        <h4>${p.name}</h4>
                        <div class="cart-item-actions">
                            <button class="qty-decrease" data-id="${p.id}">−</button>
                            <span>${item.quantity}</span>
                            <button class="qty-increase" data-id="${p.id}">+</button>
                            <span>¥${(p.price * item.quantity).toFixed(2)}</span>
                        </div>
                    </div>
                    <button class="remove-item" data-id="${p.id}">✕</button>
                </div>
            `;
        });
        cartItemsContainer.innerHTML = html;
        cartTotalPrice.textContent = `¥${getTotalPrice().toFixed(2)}`;

        // 绑定购物车内部事件
        document.querySelectorAll('.qty-decrease').forEach(btn => {
            btn.addEventListener('click', function(e) {
                const id = parseInt(this.dataset.id);
                changeQuantity(id, -1);
            });
        });
        document.querySelectorAll('.qty-increase').forEach(btn => {
            btn.addEventListener('click', function(e) {
                const id = parseInt(this.dataset.id);
                changeQuantity(id, 1);
            });
        });
        document.querySelectorAll('.remove-item').forEach(btn => {
            btn.addEventListener('click', function(e) {
                const id = parseInt(this.dataset.id);
                removeItem(id);
            });
        });
    }

    // ---------- 侧栏开关 ----------
    function openCartSidebar() {
        cartSidebar.classList.add('open');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeCartSidebar() {
        cartSidebar.classList.remove('open');
        overlay.classList.remove('active');
        document.body.style.overflow = '';
    }

    // ---------- 事件绑定 ----------
    // 商品列表 “加入购物车” (委托)
    productsGrid.addEventListener('click', function(e) {
        const btn = e.target.closest('.btn-add-cart');
        if (!btn) return;
        const id = parseInt(btn.dataset.id);
        addToCart(id);
    });

    // 购物车图标
    cartIconWrapper.addEventListener('click', function(e) {
        e.stopPropagation();
        if (cartSidebar.classList.contains('open')) {
            closeCartSidebar();
        } else {
            openCartSidebar();
        }
    });

    // 关闭按钮 & 遮罩
    closeCartBtn.addEventListener('click', closeCartSidebar);
    overlay.addEventListener('click', closeCartSidebar);

    // 模拟结算
    document.getElementById('checkoutBtn')?.addEventListener('click', function() {
        if (cart.length === 0) {
            alert('购物车是空的，请先添加商品 :)');
            return;
        }
        alert(`🧾 模拟结算！ 总金额 ¥${getTotalPrice().toFixed(2)} \n感谢在悦选购物，功能演示。`);
        // 可重置购物车 (演示)
        // cart = [];
        // updateCartUI();
        // closeCartSidebar();
    });

    // 移动端菜单切换 (汉堡)
    mobileMenuToggle.addEventListener('click', function() {
        navList.classList.toggle('open');
    });

    // 点击导航链接后关闭移动菜单 (增强体验)
    document.querySelectorAll('.nav-link').forEach(link => {
        link.addEventListener('click', function() {
            if (window.innerWidth <= 768) {
                navList.classList.remove('open');
            }
        });
    });

    // 关闭侧栏时确保菜单收起 (键盘辅助)
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            if (cartSidebar.classList.contains('open')) closeCartSidebar();
            if (navList.classList.contains('open')) navList.classList.remove('open');
        }
    });

    // ---------- 初始化 ----------
    renderProducts();
    updateCartUI();
});