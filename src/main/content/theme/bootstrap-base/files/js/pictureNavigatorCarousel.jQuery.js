(function ($) {
    // PICTURE NAVIGATOR CAROUSEL JAVASCRIPT - BR
    // Adds addtional the following functionality on top of the functioanlity provided by Twitter Bootstraps carousel
    // Adjusts number of visible thumbnails depending on the browser size
    // Allows user to select and pause thumbnails on hover
    // Allows auto updating active slide counter
    // Allows auto updating text
    // Allows clicks or touches of controls depending on the device
    // Allows "full screen" mode
    // Adds slide active slide number to the url
    // Shows the relevant slide id the url contains a #number

    $.fn.pictureNavigatorCarousel = function (parameters) {

        var settings = $.extend({ // Set parameter defaults
            timeInterval: 3000,
            changeOnHover: true
        }, parameters),
            windowWidth = window.innerWidth,
            maxLength = 41, // maximum distance that is considered a "touch"
            carouselID = "#" + $(this).attr("id"), // set the target carousel id
            split = carouselID.split("#pictureCarousel"), // get the custom class
            customClass = "." + split[1],
            url = document.location.toString(); // get url

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

        //******************************** SET UP & START CAROUSEL ********************************
        $('#full-Screen-Button').on('click', function (e) { // when full screen button is pressed
            groupThumbnails(customClass);
            fullScreen(window.innerWidth);
        });
        $('.gallery-header').on('click', function (e) { // when close full screen button is pressed
            groupThumbnails(customClass);
            notFullScreen();
        });
        $(window).bind('resize', function () { // when the browser is resized
            windowWidth = window.innerWidth;
            groupThumbnails(customClass);
            if ($('.full-screen').length) {
                fullScreen(window.innerWidth);
            }
        });
        // -------- On load ------ //
        if (windowWidth > 1023) { /* 979 LP */
            groupThumbnails(customClass); // set number of thumbnails for width of image
        }
        if (url.match('#')) { // show slide present in the url
            $(carouselID).carousel(parseInt(url.split('#')[1] - 1, 10));
            $(carouselID).carousel('pause');
        }
        $(customClass + ' #carousel-text').html($(customClass + ' #slide-content-0').html());// set the slide details for the first article
	/*added LP*/
		var activeFontSize = $('.picture-navigator-container '+ customClass + ' #carousel-text > a > *').css('font-size'); //get the font size of the active font (ONLY PIC NAVIGATOR)
		$('.picture-navigator-container '+ customClass+' #slide-content > div > a > *').css('font-size', activeFontSize); //set the hidden content to the same font size so we can detect height (ONLY PIC NAVIGATOR)
	/*added lp end*/
        var max_num_items = $(customClass + ' .carousel-inner .item').length,
            slide_num = $(customClass + ' .carousel-inner .item.active').attr('data-slide-number');
        $(customClass + ' .total-image-number').text(max_num_items);// sets max number of slides
        $(customClass + ' .active-image-number').text(parseInt(slide_num) + 1);// sets current slide number
        // start carousel
        $(carouselID).carousel({
            pause: true,
            interval: false
        });

        //********************************  CAROUSEL ACTIONS ********************************
        $(carouselID).on('slid', function (e) { // When the carousel slides, auto update the text
            var id = $(customClass + ' .item.active').data('slide-number'); // get the id of the active slide
		/*added LP*/
			$('.picture-navigator-container '+ customClass + ' #slide-content').show(); // show content to store height (ONLY PIC NAVIGATOR)
			var idHeight = $('.picture-navigator-container '+ customClass + ' #slide-content-' + id).height(); // store the height (ONLY PIC NAVIGATOR)
			console.log(idHeight);
			$('.picture-navigator-container '+ customClass + ' #slide-content').hide(); // hide content again (ONLY PIC NAVIGATOR)
			$('.picture-navigator-container '+ customClass+' #carousel-text').animate({height: idHeight+"px"}, 500);// animate to new height (ONLY PIC NAVIGATOR)
		/*added LP end*/
            $(customClass + ' #carousel-text').html($(customClass + ' #slide-content-' + id).html()); // add corresponding text to headline div

			//Customize 	 	
			$(' #carousel-headline').html($(customClass+' #carousel-text').children().first().html()); // add headline text to .gallery > h2#carousel-headline 	 	
			var extraContent = customClass+' #content-extra-' + id; // customized div to store extra hidden content 	 	
			$(' p.updated').html($(extraContent + ' > p.lastUpdated').html()); // add last updated date to .gallery > p.updated 	 	
			$('div.article-keywords').html($(extraContent + " > div.keywords").html()); // add keyword for display
			
            $(customClass + " #carousel-selector-" + id).addClass("active"); // add active class to active slide

            $(customClass + " .item").each(function (index) {
                if (index !== id) {
                    $(customClass + " #carousel-selector-" + index).removeClass("active"); // remove active class for all items that are not active
                }
            });
            showActiveThumbnailGroup(id, customClass);
			window.location.replace("#" + parseInt(id + 1, 10)); // add the picture number to the url without add to browser stack
            $(customClass + ' .active-image-number').text(+id + 1); // changes current slide number
        });

        if (isMobileDevice) { // on mobile device
            $(customClass + ' .carousel-control.left').bind('touchstart', function () { // Recognise clicks on previous
                $(carouselID).carousel('prev');
            });
            $(customClass + ' .carousel-control.right').bind('touchstart', function () { // Recognise clicks on next
                $(carouselID).carousel('next');
            });
            $(customClass + ' .carousel-control.centre').bind('touchstart', function () { // Recognise clicks on PLAY/PAUSE
                pausePlay(customClass, carouselID, settings);
            });
            $(customClass + ' .thumbnail-control.left').bind('touchstart', function () { // Recognise clicks on thumbnail next
                pushThumbnails("left", customClass);
            });
            $(customClass + ' .thumbnail-control.right').bind('touchstart', function () { // Recognise clicks on thumbnail previous
                pushThumbnails("right", customClass);
            });
        } else { // on desktop   
            $(customClass + ' .carousel-control.left').click(function () { // Recognise clicks on previous
                $(carouselID).carousel('prev');
            });
            $(customClass + ' .carousel-control.right').click(function () { // Recognise clicks on next
                $(carouselID).carousel('next');
            });
            $(customClass + ' .carousel-control.centre').click(function () { // Recognise clicks on PLAY/PAUSE
                pausePlay(customClass, carouselID, settings);
            });
            $(customClass + ' .thumbnail-control.left').click(function () { // Recognise clicks on thumbnail next
                pushThumbnails("left", customClass);
            });
            $(customClass + ' .thumbnail-control.right').click(function () { // Recognise clicks on thumbnail previous
                pushThumbnails("right", customClass);
            });
        }

        //  ******************************** THUMBNAIL ACTIONS ********************************
        if (isMobileDevice) { // on mobile device
            var startX,
                endX;
            $(customClass + ' .thumbnail').bind('touchstart', function () { // when thumbnail is touched
                startX = event.touches[0].pageX; // get start coordinates of the touch
                endX = startX;
            });
            $(customClass + ' .thumbnail').bind('touchmove', function () { // when touch moves on thumbnail               
                endX = event.touches[0].pageX; // get end coordinates of the touch
            });
            $(customClass + ' .thumbnail').bind('touchend', function () { // when touch ends                
                var difference = startX - endX; // find how far touch moved
                if (difference < maxLength && difference > maxLength * -1) { // if move was less than the max, assume touch
                    showRelevantSlide(this, carouselID);
                }
            });
        } else { // on desktop 
            if (settings.changeOnHover) { // if hover paramter is true
                $(customClass + ' [id^=carousel-selector-]').hover(function () { // on hover
                    showRelevantSlide(this, carouselID);
                    $(carouselID).carousel('pause'); // stop cycling through items
                }, function () { // when thumbnail is hovered off
                    $(carouselID).carousel({ // restart carousel
						interval: settings.timeInterval
                    });
                });
            } else { // if hover paramter is false, use clicks
                $(customClass + ' [id^=carousel-selector-]').click(function () { // on click
                    showRelevantSlide(this, carouselID);
                });
            }
        }
    };

    function pausePlay(customClass, carouselID, settings) { // pause and play actions when pause/play is pressed
        var paused = $(customClass + ' .carousel-control.centre span').hasClass('glyphicon-play');
        if (paused == true) {
            $(carouselID).carousel({ // start carousel
                interval: settings.timeInterval
            });
            $(customClass + ' .carousel-control.centre span').removeClass("glyphicon-play").addClass("glyphicon-pause"); // change icon to pause
        } else {
            $(carouselID).carousel('pause'); // pause carousel
            $(customClass + ' .carousel-control.centre span').removeClass("glyphicon-pause").addClass("glyphicon-play"); // change icon to play
        }
    }
    
    function showRelevantSlide(e, carouselID) { // show slide relevant to the thumbnail being hovered over
        var id_selector = $(e).attr("id"); // get index of article hovered over
		var id = id_selector.split("-").pop();
        id = parseInt(id, 10);
        $(carouselID).carousel(id); // show slide relevant to the thumbnail being hovered over
    }

    function groupThumbnails(customClass) { // calculates the number of required for each thumbnail group, if this value is different from that displayed, the certain thumbnails are moved from one group to another. This should be called onLoad or when the browser size changes. 
        var shouldBeDisplayed = getNumberOfThumbnails(), // set number of thumbnails for width and mode
            arrayCount = getGroupAndThumbnailCount(customClass),
            totalThumbs = arrayCount[1]; // total number of thumbnails
        for (var i = 0; i < arrayCount[0]; i++) { // every group, totalThumbs arrayCount[0]
            var currentlyDisplayed = $(customClass + ' #group-' + i + ' > li').length, // number of thumbnails currently displayed
                difference = shouldBeDisplayed - currentlyDisplayed;
            if (difference > 0) { // there should be more thumbnails per group
                for (var j = difference - 1; j > -1; j--) {
                    var thumbId = (shouldBeDisplayed - 1) - j + (shouldBeDisplayed * i); // id of the extra thumbnail
                    if (thumbId < totalThumbs) {
                        var thumnail = $(customClass + " li#thumb-" + thumbId),
                            newThumnail = thumnail.clone(true); // clone the thumbnail
                        $(customClass + " li#thumb-" + thumbId).remove(); // remove old thumbnail
                        $(customClass + " #group-" + (i)).append(newThumnail); // add new thumbnail to different group
                    };
                }
            } else if (difference < 0) { // there should be less thumbnails per group
                for (var j = difference; j < 0; j++) {
                    var thumbId = (shouldBeDisplayed - 1) - j + (shouldBeDisplayed * i), // id of the extra thumbnail
                        thumnail = $(customClass + " li#thumb-" + thumbId),
                        newThumnail = thumnail.clone(true); // clone the thumbnail
                    $(customClass + " li#thumb-" + thumbId).remove(); // remove old thumbnail
                    $(customClass + " #group-" + (i + 1)).prepend(newThumnail);
                };
            }
        };
        var id = $(customClass + ' .item.active').data('slide-number'); // get the id of the active slide
        showActiveThumbnailGroup(id, customClass);
    }
    
    function showActiveThumbnailGroup(id, customClass) { // move thumbnails to correspond with the active image
        if ($('#sliderGallery').length) { // if sliding thumbnail group exisis
            while (Math.floor(id / $(customClass + ' #group-0 > li').length) !== $(customClass + " .thumbnails.active").data('group-number')) { // while active slide is not shown in thumbnails
                if (Math.floor(id / $(customClass + ' #group-0 > li').length) > $(customClass + " .thumbnails.active").data('group-number')) { // if active slide is off to the right
                    pushThumbnails("right", customClass);
                } else if (Math.floor(id / $(customClass + ' #group-0 > li').length) < $(customClass + " .thumbnails.active").data('group-number')) { // if active slide is off to the left
                    pushThumbnails("left", customClass);
                }
            }
            $(customClass + ' .active-image-number').text(+id + 1); // changes current slide number
        }
    }
    
    function pushThumbnails(direction, customClass) { // shifts the group of thumbnails to the direction specified as a paramter (left or otherwise right)
        var array = getGroupAndThumbnailCount(customClass),
            totalGroups = array[0], // total number of groups
            totalThumbs = array[1], // total number of thumbnails
            numberOfThumbnails = getNumberOfThumbnails(); // set number of thumbnails for width and mode
        var numberOfGroups = Math.ceil(totalThumbs / numberOfThumbnails), // number of groups required
            currentSlide = $(customClass + " .thumbnails.active").data('group-number'), // get active slide
            comparitor,
            leftPosition; // new position of group
        if (direction === "left") {
            comparitor = 0; // can't move further left than slide 0
            leftPosition = "+=100%"; // left
        } else {
            comparitor = numberOfGroups - 1; // can't move further right than final slide 
            leftPosition = "-=100%"; // right
        }
        if (currentSlide !== comparitor) {
            $(customClass + " .thumbnails").each(function () {
                $(customClass + " #group-" + $(this).data('group-number')).animate({
                    left: leftPosition
                }, "slow"); // move thumbnail group
                $(customClass + " #group-" + $(this).data('group-number')).removeClass("active"); // remove active class
            });
            if (direction === "left") {
                currentSlide -= 1; // decrement active slide variable
            } else {
                currentSlide += 1; // increment active slide variable
            }
            $(customClass + " #group-" + currentSlide).addClass("active"); // add active class
        }
    }

    function getThumbnailsPerGroup(customClass) { // returns the maximum number of possible thumbnails for main image size, and remaining pixels
        var imageWidth = $(customClass + " #carousel-bounding-box").width(); // gallery width
        if (window.innerWidth < 799) { /* 767 LP */
            imageWidth += 20;
        };
        var thumbnailWidth = $(customClass + " .thumbnails > li").width(), // thumbnail width
            array = new Array();
        array[0] = Math.floor(imageWidth / thumbnailWidth); // maximum number of possible thumbnails for main image size
        array[1] = imageWidth % thumbnailWidth; // left over pixels
        return array;
    }

    function getGroupAndThumbnailCount(customClass) { // returns the total number of groups and total number of thumbnails
        var totalGroups = $(customClass + ' .thumbnails').length,
            totalThumbs = 0;
        for (var i = 0; i < totalGroups; i++) {
            totalThumbs += $(customClass + ' #group-' + i + ' > li').length; // count every thumbnail isnide every group
        }
        var array = new Array();
        array[0] = totalGroups;
        array[1] = totalThumbs;
        return array;
    }

    function fullScreen(width) { // called when entering full screen mode
        if (width > $(window).height() && width < 799) { /* 767 LP */ // if viewed in landscape on "tablets" or smaller
            $(".gallery-wrapper").addClass("landscape");
        } else {    
            $(".gallery-wrapper").removeClass("landscape");
        }

        var containerHeight = $(".gallery-container").height(),
            galleryTopHeight = $(".gallery-top").outerHeight();
        var thumbHeight = $("#slider-thumbs").outerHeight(),
            textHeight = $("#carousel-text").outerHeight(),
            shareHeight = $(".article-share.in-gallery").outerHeight() +30;
        if (width > $(window).height() && width < 799) { /* 767 LP */ // small landscape
            textHeight -= 10;
        }
        var maxHeight;
        if($(".gallery-wrapper").hasClass("inline"))
        {
            maxHeight = containerHeight - (galleryTopHeight + thumbHeight + textHeight + 10);
            if (width < 1024) { /* 980 LP */
                maxHeight += 46;
            }
        }else
        {
            maxHeight = containerHeight - (galleryTopHeight + thumbHeight + 50);   
            if (width < 1024) { /* 980 LP */
                maxHeight = containerHeight - (galleryTopHeight + thumbHeight + textHeight + shareHeight + 50);
                if (width > $(window).height() && width < 799) { /* 767 LP */ // if viewed in landscape on "tablets" or smaller
                    maxHeight = containerHeight - (galleryTopHeight + shareHeight);
                }
            }
        }
        $('#carousel-bounding-box').css('maxWidth', (maxHeight * 1.6) + "px"); // set size of main image
    }

    function notFullScreen() { // called when leaving full screen mode
        $('#carousel-bounding-box').css('maxWidth', ""); // remove size of main image
    }
    
    function getNumberOfThumbnails() { // get how many thumbnails should be displayed depending on the "mode" and width of the device
        var numberOfThumbnails = 3;
        if ($('.full-screen').length) {
            if (window.innerWidth > 615) {
                numberOfThumbnails = 4;
            }
            if (window.innerWidth > 1140) {
                numberOfThumbnails = 5;
            }
            if ((window.innerWidth > 820 && window.innerWidth < 1023)) { /* 979 LP */
                numberOfThumbnails = 5;
            }
        } else {
            if ( window.innerWidth > 1023) { /* 979 LP */
                numberOfThumbnails = 4;
            }
        }
        return numberOfThumbnails;
    }
})(jQuery);