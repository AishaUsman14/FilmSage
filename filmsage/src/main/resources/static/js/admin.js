// Load dashboard by default when the page loads
document.addEventListener('DOMContentLoaded', () => {
    // Load the default section (dashboard) initially
    showSection('dashboard');
});

function showSection(sectionId) {
    // Hide all sections
    document.querySelectorAll('.section').forEach(section => {
        section.style.display = 'none';
    });
    
    // Show selected section
    const sectionElement = document.getElementById(sectionId);
    if (sectionElement) {
        sectionElement.style.display = 'block';
    }
    
    // Load data for the section
    if (sectionId === 'dashboard') {
        loadDashboardStats();
    } else if (sectionId === 'users') {
        loadUsers();
    } else if (sectionId === 'reviews') {
        loadPendingReviews();
    } // Add cases for 'actors' and 'movies' here later if needed
}

// --- Dashboard Analytics Functions --- 
function loadDashboardStats() {
    fetch('/api/admin/stats')
        .then(response => response.json())
        .then(stats => {
            const statsContainer = document.querySelector('#dashboard .stats-container');
            if (!statsContainer) return; 

            // Clear previous stats
            statsContainer.innerHTML = '';

            // Create and append stat cards
            statsContainer.appendChild(createStatCard('Total Users', stats.totalUsers, 'fas fa-users'));
            statsContainer.appendChild(createStatCard('Total Ratings', stats.totalRatings, 'fas fa-star'));
            statsContainer.appendChild(createStatCard('Pending Reviews', stats.pendingReviews, 'fas fa-hourglass-half'));
            statsContainer.appendChild(createStatCard('Approved Reviews', stats.approvedReviews, 'fas fa-check-circle'));
            statsContainer.appendChild(createStatCard('Rejected Reviews', stats.rejectedReviews, 'fas fa-times-circle'));
            statsContainer.appendChild(createStatCard('Avg. Rating (Approved)', stats.averageRating.toFixed(1), 'fas fa-star-half-alt'));
            statsContainer.appendChild(createStatCard('Watchlist Items', stats.totalWatchlistItems, 'fas fa-list-ul'));
            
            // Add more stats as needed

        })
        .catch(error => {
            console.error('Error loading dashboard stats:', error);
            const statsContainer = document.querySelector('#dashboard .stats-container');
            if (statsContainer) {
                statsContainer.innerHTML = '<p>Error loading dashboard statistics.</p>';
            }
        });
}

// Helper function to create a stat card element
function createStatCard(title, value, iconClass) {
    const card = document.createElement('div');
    card.className = 'stat-card';
    
    const icon = document.createElement('i');
    icon.className = `${iconClass} stat-icon`;

    const content = document.createElement('div');
    content.className = 'stat-content';
    
    const valueEl = document.createElement('div');
    valueEl.className = 'stat-value';
    valueEl.textContent = value !== undefined && value !== null ? value : 'N/A'; // Handle potential null/undefined
    
    const titleEl = document.createElement('div');
    titleEl.className = 'stat-title';
    titleEl.textContent = title;

    content.appendChild(valueEl);
    content.appendChild(titleEl);
    card.appendChild(icon);
    card.appendChild(content);
    
    return card;
}


// --- User Management Functions ---
function loadUsers() {
    fetch('/api/admin/users')
        .then(response => response.json())
        .then(users => {
            const userList = document.querySelector('.user-list');
            if (!userList) return; // Guard clause
            userList.innerHTML = users.map(user => {
                // Check if user has admin role
                const isAdmin = user.roles && user.roles.some(role => role.name === 'ROLE_ADMIN');
                
                return `
                <div class="user-card">
                    <div class="user-info">
                        <h3>${user.username}</h3>
                        <p>${user.email}</p>
                        <p class="status ${user.enabled ? 'status-active' : 'status-suspended'}">
                            Status: ${user.enabled ? 'Active' : 'Suspended'}
                        </p>
                    </div>
                    <div class="user-actions">
                        ${!isAdmin ? `
                            ${user.enabled 
                                ? `<button class="suspend-btn" onclick="suspendUser(${user.id})">Suspend</button>`
                                : `<button class="activate-btn" onclick="activateUser(${user.id})">Activate</button>`
                            }
                            <button class="delete-btn" onclick="deleteUser(${user.id})">Delete</button>
                        ` : '<span class="admin-badge">Admin Account</span>'}
                    </div>
                </div>
            `}).join('');
        })
        .catch(error => console.error('Error loading users:', error));
}

function suspendUser(userId) {
    fetch(`/api/admin/users/${userId}/suspend`, { method: 'POST' })
        .then(() => loadUsers())
        .catch(error => console.error('Error suspending user:', error));
}

function activateUser(userId) {
    fetch(`/api/admin/users/${userId}/activate`, { method: 'POST' })
        .then(() => loadUsers())
        .catch(error => console.error('Error activating user:', error));
}

function deleteUser(userId) {
    if (confirm('Are you sure you want to delete this user?')) {
        fetch(`/api/admin/users/${userId}`, { method: 'DELETE' })
            .then(() => loadUsers())
            .catch(error => console.error('Error deleting user:', error));
    }
}

// --- Content Moderation Functions ---
function loadPendingReviews() {
    fetch('/api/admin/reviews/pending')
        .then(response => response.json())
        .then(reviews => {
            const reviewList = document.querySelector('.review-list');
            if (!reviewList) return; // Guard clause

            if (reviews.length === 0) {
                reviewList.innerHTML = '<p>No pending reviews found.</p>';
                return;
            }

            reviewList.innerHTML = reviews.map(review => `
                <div class="review-card" id="review-${review.id}">
                    <div class="review-info">
                        <p><strong>User:</strong> ${review.username || 'N/A'}</p>
                        <p><strong>Movie ID:</strong> ${review.movieId || 'N/A'}</p> 
                        <p><strong>Review:</strong></p>
                        <p class="review-text">${review.review || ''}</p>
                        <p><small>Submitted: ${review.createdAt ? new Date(review.createdAt).toLocaleString() : 'N/A'}</small></p>
                    </div>
                    <div class="review-actions">
                        <button class="approve-btn" onclick="approveReview(${review.id})">Approve</button>
                        <button class="reject-btn" onclick="rejectReview(${review.id})">Reject</button>
                    </div>
                </div>
            `).join('');
        })
        .catch(error => {
            console.error('Error loading pending reviews:', error);
            const reviewList = document.querySelector('.review-list');
            if (reviewList) {
                reviewList.innerHTML = '<p>Error loading reviews. Please try again later.</p>';
            }
        });
}

function approveReview(reviewId) {
    fetch(`/api/admin/reviews/${reviewId}/approve`, { method: 'PUT' })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to approve review');
            }
            // Refresh the list or remove the card directly
            // For simplicity, we reload the list
            loadPendingReviews(); 
        })
        .catch(error => console.error('Error approving review:', error));
}

function rejectReview(reviewId) {
    fetch(`/api/admin/reviews/${reviewId}/reject`, { method: 'PUT' })
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to reject review');
            }
            // Refresh the list or remove the card directly
            loadPendingReviews(); 
        })
        .catch(error => console.error('Error rejecting review:', error));
} 