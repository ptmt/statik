/**
 * Abbreviation Popups - Click-to-toggle popups for <abbr> elements
 * Converts browser tooltips to accessible click-based popovers
 */
(function() {
  'use strict';

  let activePopup = null;
  let activeAbbr = null;

  /**
   * Create and show popup for an abbr element
   */
  function showPopup(abbr) {
    // Remove any existing popup
    hidePopup();

    const title = abbr.getAttribute('title');
    if (!title) return;

    // Create popup element
    const popup = document.createElement('span');
    popup.className = 'abbr-popup';
    popup.textContent = title;
    popup.setAttribute('role', 'tooltip');

    // Temporarily hide title to prevent browser tooltip
    abbr.dataset.originalTitle = title;
    abbr.removeAttribute('title');

    // Position relative to abbr
    abbr.style.position = 'relative';
    abbr.appendChild(popup);

    // Check if popup is cut off and adjust position
    const rect = popup.getBoundingClientRect();
    if (rect.left < 10) {
      popup.classList.add('flip-left');
    } else if (rect.right > window.innerWidth - 10) {
      popup.classList.add('flip-right');
    }

    activePopup = popup;
    activeAbbr = abbr;
  }

  /**
   * Hide the active popup
   */
  function hidePopup() {
    if (activePopup && activeAbbr) {
      // Restore title attribute
      if (activeAbbr.dataset.originalTitle) {
        activeAbbr.setAttribute('title', activeAbbr.dataset.originalTitle);
        delete activeAbbr.dataset.originalTitle;
      }
      activePopup.remove();
      activePopup = null;
      activeAbbr = null;
    }
  }

  /**
   * Toggle popup on click
   */
  function handleClick(e) {
    const abbr = e.target.closest('abbr[title], abbr[data-original-title]');

    if (abbr) {
      e.preventDefault();
      e.stopPropagation();

      // If clicking the same abbr, toggle off
      if (activeAbbr === abbr) {
        hidePopup();
      } else {
        // Restore title if it was hidden
        if (abbr.dataset.originalTitle && !abbr.getAttribute('title')) {
          abbr.setAttribute('title', abbr.dataset.originalTitle);
        }
        showPopup(abbr);
      }
    } else {
      // Clicked outside - hide popup
      hidePopup();
    }
  }

  /**
   * Handle keyboard navigation
   */
  function handleKeydown(e) {
    // Close on Escape
    if (e.key === 'Escape' && activePopup) {
      hidePopup();
      if (activeAbbr) {
        activeAbbr.focus();
      }
    }

    // Toggle on Enter/Space when abbr is focused
    if ((e.key === 'Enter' || e.key === ' ') && e.target.matches('abbr[title]')) {
      e.preventDefault();
      if (activeAbbr === e.target) {
        hidePopup();
      } else {
        showPopup(e.target);
      }
    }
  }

  /**
   * Make abbr elements focusable
   */
  function setupAccessibility() {
    document.querySelectorAll('abbr[title]').forEach(abbr => {
      if (!abbr.hasAttribute('tabindex')) {
        abbr.setAttribute('tabindex', '0');
      }
      abbr.setAttribute('role', 'button');
      abbr.setAttribute('aria-expanded', 'false');
    });
  }

  // Initialize
  document.addEventListener('click', handleClick);
  document.addEventListener('keydown', handleKeydown);

  // Setup on load and after dynamic content changes
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', setupAccessibility);
  } else {
    setupAccessibility();
  }

  // Re-run setup when content might change (for SPAs or dynamic content)
  const observer = new MutationObserver(function(mutations) {
    mutations.forEach(function(mutation) {
      if (mutation.addedNodes.length) {
        setupAccessibility();
      }
    });
  });

  observer.observe(document.body, { childList: true, subtree: true });
})();
