var windowBig = true;

function searchFilter(windowWidth) {

  if (windowWidth < 768) {
    // Click function to show/hide search filters
    jQuery(".search-filters-outer-container h3").click(function(e) {
      jQuery(".search-filters-inner-container").slideToggle();
      e.preventDefault();
    });

    jQuery(".search-filters-inner-container").hide();
    jQuery(".search-filters-outer-container").appendTo(".search-sort-filter");

  } else if (windowWidth > 767) {
    // Prevent click function used for small screen 
    jQuery(".search-filters-outer-container h3").off();

    jQuery(".search-filters-inner-container").show();
    jQuery(".search-filters-outer-container").appendTo(
        ".content-b .inner-d article");

  }

}

$(function() {
  var windowWidth = window.innerWidth;
  searchFilter(windowWidth);
  jQuery(window).resize(function() {
      var newWindowWidth = window.innerWidth;
  
      if ((newWindowWidth > 767 && windowWidth < 768)
          || (newWindowWidth < 768 && windowWidth > 767)) {
        searchFilter(newWindowWidth);
      }
      windowWidth = newWindowWidth;
  });
});

function searchFromDate(e) {
  e.preventDefault();
  var $this = $(e.target);
  var fromDate = $this.data('from-date');
  var $form = $(e.target).parents('form');
  
  $form.find('input[name="fromDate"]').val(fromDate);
  $form.submit();
}

function filterByDate(e) {
  e.preventDefault();
  var $this = $(e.target);
  var $form = $(e.target).parents('form');
  var fromDate = $form.find('input[name="filterFromDate"]').val();
  var toDate = $form.find('input[name="filterToDate"]').val();

  $form.find('input[name="fromDate"]').val(fromDate);
  $form.find('input[name="toDate"]').val(toDate);
  
  $form.submit();
}