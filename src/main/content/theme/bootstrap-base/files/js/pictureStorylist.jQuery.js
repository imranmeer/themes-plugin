(function ($) {
    // PICTURE STORYLIST CAROUSEL JAVASCRIPT
    $.fn.pictureStorylist = function (parameters) {

        var settings = $.extend({ // Set parameter defaults
            timeInterval: 3000,
            changeOnHover: true,
			itemClass: "",
			containerClass: "",
			numberOfThumbsMob: 1,
			numberOfThumbsDesk: 4,
			targetID: ""
        }, parameters),
            windowWidth = window.innerWidth,
            carouselID = "#" + $(this).attr("id"), // set the target carousel id
            split = carouselID.split(settings.targetID), // get the custom class
            customClass = "." + split[1];

        // var isMobileDevice = ? ? ? ? // This variable needs to be set to true if the user views the page on a tablet or phone (touch device)
        // It should be set to false when when a user views the page on a desktop machine. Setting this variable on device width alone is not accurate enough.

        // ***** ----- CODE FOR TESTING START ----- *****
        // The next 4 lines are for the purpose of testing only - DO NOT INCLUDE THEM IN THE FINAL CODE. 
        // The are used instead of setting the isMobileDevice variable, when a solution is found, this code can be removed
        var isMobileDevice;
        if (window.innerWidth < 10) {
            isMobileDevice = true;
        }
        // ***** ----- CODE FOR TESTING END ----- *****

        //******************************** SET UP & START CAROUSEL *******************************
		
        // -------- On load ------ //
        if (windowWidth > 1023) {
            groupThumbnails(customClass, settings); // set number of thumbnails for width of image
        } 

		$(carouselID).carousel({
			pause: true,
			interval: false
		});

        //********************************  CAROUSEL ACTIONS ********************************

        if (isMobileDevice) { // on mobile device
            $(customClass + ' .carousel-control.left').bind('touchstart', function () { // Recognise clicks on previous
                $(carouselID).carousel('prev');
            });
            $(customClass + ' .carousel-control.right').bind('touchstart', function () { // Recognise clicks on next
                $(carouselID).carousel('next');
            });
        } else { // on desktop   
            $(customClass + ' .carousel-control.left').click(function () { // Recognise clicks on previous
                $(carouselID).carousel('prev');
            });
            $(customClass + ' .carousel-control.right').click(function () { // Recognise clicks on next
                $(carouselID).carousel('next');
            });
        }
		
		preventSlide(customClass, carouselID, settings);
		
		$('.picture-navigator').on('slid.bs.carousel', function () {
			$(customClass + ' .carousel-control.right,' + customClass + ' .carousel-control.left').show();
			preventSlide(customClass, carouselID, settings);
		});
		$(window).resize(function() {
			groupThumbnails(customClass, settings);
			$(customClass + ' .carousel-control.right,' + customClass + ' .carousel-control.left').show();
			preventSlide(customClass, carouselID, settings);
		});
    };

	//////////////////////////////////////////////////////////////////////////////
	// Picture Storylist/Events Functions
	//////////////////////////////////////////////////////////////////////////////

	function groupThumbnails(customClass, settings) { // calculates the number of required for each thumbnail group, if this value is different from that displayed, the certain thumbnails are moved from one group to another. This should be called onLoad or when the browser size changes.
		var shouldBeDisplayed = getNumberOfItems(settings), // set number of thumbnails for width and mode
            totalThumbs = $(customClass + " ." + settings.itemClass).length; // total number of thumbnails
        for (var i = 0; i < $(customClass + " ." + settings.containerClass).length; i++) { // every group, totalThumbs arrayCount[0]
            var currentlyDisplayed = $(customClass + " ." + settings.containerClass).eq(i).find(" ." + settings.itemClass).length, // number of thumbnails currently displayed
            difference = shouldBeDisplayed - currentlyDisplayed;
            if (difference > 0) { // there should be more thumbnails per group
                for (var j = difference; j > -1; j--) {
                    var thumbId = (shouldBeDisplayed) - j + (shouldBeDisplayed * i); // id of the extra thumbnail
                    if (thumbId < totalThumbs) {
                        var thumnail = $(customClass + " ." + settings.itemClass).eq(thumbId),
                            newThumnail = thumnail.clone(true); // clone the thumbnail
                        $(customClass + " ." + settings.itemClass).eq(thumbId).remove(); // remove old thumbnail
                        $(customClass + " ." + settings.containerClass).eq(i).append(newThumnail); // add new thumbnail to different group
                    };
                }
            } else if (difference < 0) { // there should be less thumbnails per group
                for (var j = difference; j < 0; j++) {
                    var thumbId = (shouldBeDisplayed - 1) - j + (shouldBeDisplayed * i); // id of the extra thumbnail
                        thumnail = $(customClass + " ." + settings.itemClass).eq(thumbId);
                        newThumnail = thumnail.clone(true); // clone the thumbnail
                    $(customClass + " ." + settings.itemClass).eq(thumbId).remove(); // remove old thumbnail
                    $(customClass + " ." + settings.containerClass).eq(i + 1).prepend(newThumnail);
                };
            }
        };
    }
	
	function getNumberOfItems(settings) { // get how many thumbnails should be displayed on mobile and desktop
        var numberOfThumbnails = settings.numberOfThumbsMob;
        
            if (window.innerWidth > 1023) {
                numberOfThumbnails = settings.numberOfThumbsDesk;
            }
        
        return numberOfThumbnails;
    }
	
	function preventSlide(customClass, carouselID, settings) {
		var totalThumbs = $(customClass + " ." + settings.itemClass).length;
		var numberOfGroupsShould = Math.ceil( totalThumbs / getNumberOfItems(settings) );
		var currentGroup = $(customClass + " .item.active").data("slide-number");
		
		if( currentGroup == (numberOfGroupsShould - 1) ){
			$(customClass + ' .carousel-control.right').hide();
		} 

		if( currentGroup == 0 ){
			$(customClass + ' .carousel-control.left').hide();
		}
	
		if( $(customClass + ' .item.active' + " ." + settings.containerClass + " ." + settings.itemClass).length == 0 ){
			$(carouselID).carousel(numberOfGroupsShould - 1);
			$(carouselID).carousel('pause');
		}
	}
	
})(jQuery);