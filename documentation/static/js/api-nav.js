/**
 * API Navigation - Smooth scrolling and active state management
 * for config.json reference page
 */
(function() {
  'use strict';

  // Only run on pages with API navigation
  const apiSubmenu = document.querySelector('.api-submenu');
  if (!apiSubmenu) return;

  const apiLinks = document.querySelectorAll('.api-link');
  const apiItems = document.querySelectorAll('.api-item');

  /**
   * Update active state for sidebar links
   */
  function updateActiveApiLink(id) {
    apiLinks.forEach(link => {
      const linkId = link.getAttribute('href').slice(1);
      if (linkId === id) {
        link.classList.add('active');
      } else {
        link.classList.remove('active');
      }
    });
  }

  /**
   * Smooth scroll to API section on click
   */
  apiLinks.forEach(link => {
    link.addEventListener('click', function(e) {
      e.preventDefault();
      const targetId = this.getAttribute('href').slice(1);
      const target = document.getElementById(targetId);

      if (target) {
        // Smooth scroll to target
        target.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });

        // Update URL without jumping
        if (history.pushState) {
          history.pushState(null, '', '#' + targetId);
        }

        // Update active state
        updateActiveApiLink(targetId);
      }
    });
  });

  /**
   * Intersection Observer for scroll-based highlighting
   * Highlights the sidebar link for the currently visible section
   */
  const observerOptions = {
    root: null,
    // Trigger when section is in the middle 20-30% of viewport
    rootMargin: '-20% 0px -70% 0px',
    threshold: 0
  };

  const observer = new IntersectionObserver(function(entries) {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        updateActiveApiLink(entry.target.id);
      }
    });
  }, observerOptions);

  // Observe all API items
  apiItems.forEach(item => {
    if (item.id) {
      observer.observe(item);
    }
  });

  /**
   * Handle direct navigation with hash on page load
   */
  function handleInitialHash() {
    const hash = window.location.hash;
    if (hash) {
      const targetId = hash.slice(1);
      const target = document.getElementById(targetId);

      if (target) {
        // Small delay to ensure page is fully loaded
        setTimeout(function() {
          target.scrollIntoView({ behavior: 'smooth' });
          updateActiveApiLink(targetId);
        }, 100);
      }
    }
  }

  // Run on page load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', handleInitialHash);
  } else {
    handleInitialHash();
  }
})();
