// script.js — 完整的交互逻辑 (模块化风格)

(() => {
  'use strict';

  // ---------- 商品数据 ----------
  const productList = [
    {
      id: 1,
      title: '降噪无线耳机',
      category: '电子产品',
      price: 499,
      image: 'https://picsum.photos/seed/headphone/400/300',
    },
    {
      id: 2,
      title: '北欧简约台灯',
      category: '家居',
      price: 299,
      image: 'https://picsum.photos/seed/lamp/400/300',
    },
    {
      id: 3,
      title: '有机棉休闲T恤',
      category: '服饰',
      price: 149,
      image: 'https://picsum.photos/seed/tshirt/400/300',
    },
    {
      id: 4,
      title: '智能运动手环',
      category: '电子产品',
      price: 199,
      image: 'https://picsum.photos/seed/band/400/300',
    },
    {
      id: 5,
      title: '陶瓷马克杯套装',
      category: '家居',
      price: 129,
      image: 'https://picsum.photos/seed/cup/400/300',
    },
    {
      id: 6,
      title: '复古帆布双肩包',
      category: '服饰',
      price: 359,
      image: 'https://picsum.photos/seed/backpack/400/300',
    },
  ];

  // ---------- 购物车状态 ----------
  let cartItems = [];

  // DOM 元素
  const productGrid = document.getElementById('productGrid');
  const cartSidebar = document.getElementById('cartSidebar');
  const cartBadge = document.getElementById('cartBadge');
  const cartItemsContainer = document.getElementById('cartItemsContainer');
  const cartTotalPrice = document.getElementById('cartTotalPrice');
  const checkoutBtn = document.getElementById('checkoutBtn');
  const closeCartBtn = document.getElementById('closeCart');
  const cartToggle = document.getElementById('cartToggle');
  const overlay = document.getElementById('overlay');
  const navLinks = document.querySelectorAll('.nav-link');

  // ---------- 渲染商品 (根据分类) ----------
  function renderProducts(category = 'all') {
    productGrid.innerHTML = '';
    const filtered = category === 'all'
      ? productList
      : productList.filter(p => p.category === category);

    if (filtered.length === 0) {
      productGrid.innerHTML = `<p style="text-align:center; width:100%; padding:3rem 0; color:#64748b;">⏳ 暂无此类商品，逛逛其他分类吧</p>`;
      return;
    }

    filtered.forEach(product => {
      const card = document.createElement('div');
      card.className = 'product-card';
      card.dataset.id = product.id;

      card.innerHTML = `
        <img class="product-image" src="${product.image}" alt="${product.title}" loading="lazy">
        <div class="product-info">
          <div class="product-title">${product.title}</div>
          <span class="product-category">${product.category}</span>
          <div class="product-price">¥${product.price}</div>
          <button class="add-to-cart-btn" data-id="${product.id}">加入购物车</button>
        </div>
      `;

      productGrid.appendChild(card);
    });

    // 为所有“加入购物车”按钮绑定事件 (事件委托也可，但直接绑定更清晰)
    document.querySelectorAll('.add-to-cart-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const productId = parseInt(e.target.dataset.id, 10);
        const product = productList.find(p => p.id === productId);
        if (product) addToCart(product);
      });
    });
  }

  // ---------- 购物车核心操作 ----------
  function addToCart(product) {
    const existing = cartItems.find(item => item.id === product.id);
    if (existing) {
      existing.quantity += 1;
    } else {
      cartItems.push({ ...product, quantity: 1 });
    }
    updateCartUI();
    // 反馈提示
    const btn = document.querySelector(`.add-to-cart-btn[data-id="${product.id}"]`);
    if (btn) {
      const originalText = btn.textContent;
      btn.textContent = '✓ 已加入';
      btn.style.background = '#16a34a';
      setTimeout(() => {
        btn.textContent = originalText;
        btn.style.background = '#2563eb';
      }, 600);
    }
  }

  function removeFromCart(productId) {
    cartItems = cartItems.filter(item => item.id !== productId);
    updateCartUI();
  }

  function updateCartUI() {
    // 更新角标
    const totalCount = cartItems.reduce((acc, item) => acc + item.quantity, 0);
    cartBadge.textContent = totalCount;

    // 渲染购物车列表
    if (cartItems.length === 0) {
      cartItemsContainer.innerHTML = `<p class="empty-cart-msg">🛒 购物车是空的，快去逛逛吧</p>`;
      checkoutBtn.textContent = '结算 (0)';
      checkoutBtn.disabled = true;
      cartTotalPrice.textContent = '¥0';
      return;
    }

    checkoutBtn.disabled = false;
    let fragment = document.createDocumentFragment();
    let total = 0;

    cartItems.forEach(item => {
      const itemTotal = item.price * item.quantity;
      total += itemTotal;

      const div = document.createElement('div');
      div.className = 'cart-item';
      div.innerHTML = `
        <img class="cart-item-img" src="${item.image}" alt="${item.title}">
        <div class="cart-item-details">
          <div class="cart-item-title">${item.title} <span style="color:#64748b;font-size:0.8rem;">×${item.quantity}</span></div>
          <div class="cart-item-price">¥${item.price} <span style="font-weight:600;">(小计 ¥${itemTotal})</span></div>
        </div>
        <button class="cart-item-remove" data-id="${item.id}">移除</button>
      `;
      fragment.appendChild(div);
    });

    cartItemsContainer.innerHTML = '';
    cartItemsContainer.appendChild(fragment);

    // 绑定移除按钮
    document.querySelectorAll('.cart-item-remove').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const id = parseInt(e.target.dataset.id, 10);
        removeFromCart(id);
      });
    });

    // 更新总计 & 结算按钮
    cartTotalPrice.textContent = `¥${total}`;
    checkoutBtn.textContent = `结算 (${totalCount})`;
  }

  // ---------- 购物车侧栏 & 遮罩 ----------
  function openCart() {
    cartSidebar.classList.add('open');
    overlay.classList.add('active');
    document.body.style.overflow = 'hidden';
  }

  function closeCart() {
    cartSidebar.classList.remove('open');
    overlay.classList.remove('active');
    document.body.style.overflow = '';
  }

  // ---------- 分类切换 (导航) ----------
  function setActiveCategory(category) {
    navLinks.forEach(link => {
      const cat = link.dataset.category;
      link.classList.toggle('active', cat === category);
    });
    renderProducts(category);
  }

  // ---------- 事件绑定 ----------
  // 导航分类
  navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const category = e.target.dataset.category;
      setActiveCategory(category);
    });
  });

  // 购物车图标与遮罩关闭
  cartToggle.addEventListener('click', openCart);
  closeCartBtn.addEventListener('click', closeCart);
  overlay.addEventListener('click', closeCart);

  // 键盘可访问性: ESC关闭侧栏
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && cartSidebar.classList.contains('open')) {
      closeCart();
    }
  });

  // 结算按钮演示
  checkoutBtn.addEventListener('click', () => {
    if (cartItems.length === 0) return;
    alert(`🛒 订单总金额: ${cartTotalPrice.textContent}，感谢购买！(演示功能)`);
    // 模拟结算清空购物车
    cartItems = [];
    updateCartUI();
    closeCart();
  });

  // ---------- 初始化 ----------
  function init() {
    renderProducts('all');
    updateCartUI();
    // 默认高亮“全部”
    navLinks.forEach(link => {
      if (link.dataset.category === 'all') link.classList.add('active');
    });
  }

  init();
})();