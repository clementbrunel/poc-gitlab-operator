/**
 * Global loading overlay management
 */

// Create loading overlay element
function createLoadingOverlay() {
    const overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.className = 'loading-overlay';
    overlay.innerHTML = `
        <div class="loading-content">
            <div class="loading-spinner"></div>
            <div class="loading-text">Chargement des versions...</div>
        </div>
    `;
    document.body.appendChild(overlay);
    return overlay;
}

// Show loading overlay
function showLoading() {
    let overlay = document.getElementById('loadingOverlay');
    if (!overlay) {
        overlay = createLoadingOverlay();
    }
    overlay.classList.add('active');
}

// Hide loading overlay
function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.classList.remove('active');
    }
}

// Attach loading to specific links
function attachLoadingToLinks(selector) {
    document.querySelectorAll(selector).forEach(link => {
        link.addEventListener('click', function(e) {
            // Only show loading if it's a navigation (not disabled)
            if (!this.classList.contains('disabled') &&
                !this.hasAttribute('disabled') &&
                this.href && this.href !== '#') {
                showLoading();
            }
        });
    });
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Create the overlay
    createLoadingOverlay();

    // Hide loading when page is fully loaded
    window.addEventListener('load', hideLoading);

    // Hide loading if user navigates back
    window.addEventListener('pageshow', function(event) {
        if (event.persisted) {
            hideLoading();
        }
    });
});
