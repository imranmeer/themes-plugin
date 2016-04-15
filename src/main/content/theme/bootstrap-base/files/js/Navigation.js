jQuery(".navbar-primary-container .nav li > span").click(function() {
    jQuery(this).toggleClass("nav-arrow-open").toggleClass("nav-arrow"); // Changes arrow 
    jQuery(this).parent().find(".nav").eq(0).slideToggle(); // Opens navigation
});

if (jQuery("li").hasClass("active")) {
    jQuery("li.active > span").toggleClass("nav-arrow-open").toggleClass("nav-arrow");
}

$('.primary.nav > li').hover( // If the dropdown is too wide move the starting left position so it will fit
function() {
    if ($(this).children().length > 3) { // has dropdown
        width = $(this).find(".primary-drop .drop-content-a").outerWidth(true) + $(this).find(".primary-drop .drop-content-b").outerWidth(true); // width is left column + right column + 43 for padding/borders
        ulLeft = $(".navbar-secondary-container .secondary.nav").offset().left; // left edge of navigation
        ulRight = parseFloat(ulLeft + $(".navbar-secondary-container .secondary.nav").outerWidth(true)); // right edge (left edge + width)
        if (width > ulRight - $(this).offset().left) { //required width > avaliable width 
            left = (ulRight - width) - ulLeft;
            $(this).find(".nav.primary-drop").css("left", left + "px");
        }
    }
});

$("#topLink").click(function() {
  $("html, body").animate({ scrollTop: 0 }, 500);
  return false;
});

/* JS to close the share popover, when option is selected or when user clicks off popover */
$( ".popover-content > div > a" ).click(function() {
    $('#share-trigger-top').popover('hide');
});

$("body").click(function(e) {
	if(e.target.className !== "text" && e.target.id !== "share-trigger-top"){
		$('#share-trigger-top').popover('hide');
	}
});