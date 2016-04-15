(function ($) {
    // SWIPE CAROUSEL JAVASCRIPT - BR
    // Adds swipe functionality to carousels, and triggers the the slide to change 

    // define the commands that can be used  
    var commands = {
        touchStart: touchStart, // detects a single finger touch start position
        touchMove: touchMove, // detects the touch end position
        touchEnd: touchEnd, // calculates the angle and direction of the swipe and performs actions accordingly
        touchCancel: touchCancel // resets variables
    },
        carousel,
        slider,
        customClass,
        triggerElementID = null, // this variable is used to identity the triggering element
        fingerCount = 0,
        startX = 0,
        startY = 0,
        curX = 0,
        curY = 0,
        deltaX = 0,
        deltaY = 0,
        horzDiff = 0,
        vertDiff = 0,
        minLength = 72, // the shortest distance the user may swipe
        swipeLength = 0,
        swipeAngle = null,
        swipeDirection = null;

    $.fn.swipeCarousel = function () {
        var id = "#" + $(this).attr("id"), // set the target carousel id
            split = id.split("#pictureCarousel"); // get the custom class if carousel
        carousel = 0;
        slider = 0;
        if (split.length > 1) {
            carousel = id;
        } else {
            split = id.split("#slider"); // get the custom class if slider
            slider = id;
        }
        customClass = "." + split[1];
        customClass = customClass.toLowerCase();
        if (typeof arguments[0] === 'string') { //execute string comand on swipeCarousel  
            var property = arguments[1],//remove the command name from the arguments  
                args = Array.prototype.slice.call(arguments);
            args.splice(0, 1);
            commands[arguments[0]].apply(this, args);
        }
        return this;
    };

    // Exposed functions  
    // The 4 Touch Event Handlers
    // NOTE: the touchStart handler should also receive the ID of the triggering element make sure its ID is passed in the event call placed in the element declaration, like: <div id="picture-frame" ontouchstart="touchStart(event,'picture-frame');"  ontouchend="touchEnd(event);" ontouchmove="touchMove(event);" ontouchcancel="touchCancel(event);"> get the total number of fingers touching the screen
    function touchStart(event, passedName) {
        fingerCount = event.touches.length;
        if (fingerCount === 1) { // check that only one finger was used
            startX = event.touches[0].pageX; // get the coordinates of the touch
            startY = event.touches[0].pageY;
            triggerElementID = passedName; // store the triggering element ID
        } else {
            touchCancel(event); // more than one finger touched so cancel
        }
    }

    function touchMove(event) {
        if (event.touches.length === 1) {
            curX = event.touches[0].pageX;
            curY = event.touches[0].pageY;
        } else {
            touchCancel(event);
        }
    }

    function touchEnd(event) {
        if (fingerCount === 1 && curX !== 0) { // check to see if more than one finger was used and that there is an ending coordinate
            swipeLength = Math.round(Math.sqrt(Math.pow(curX - startX, 2) + Math.pow(curY - startY, 2))); // use the Distance Formula to determine the length of the swipe
            if (swipeLength >= minLength) { // if the user swiped more than the minimum length, perform the appropriate action
                caluculateAngle();
                determineSwipeDirection();
                processingRoutine();
                touchCancel(event); // reset the variables
            } else {
                touchCancel(event);
            }
        } else {
            touchCancel(event);
        }
    }

    function touchCancel(event) {
        fingerCount = 0; // reset the variables back to default values
        startX = 0;
        startY = 0;
        curX = 0;
        curY = 0;
        deltaX = 0;
        deltaY = 0;
        horzDiff = 0;
        vertDiff = 0;
        swipeLength = 0;
        swipeAngle = null;
        swipeDirection = null;
        triggerElementID = null;
    }

    function caluculateAngle() {
        var X = startX - curX,
            Y = curY - startY,
            Z = Math.round(Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2))), //the distance - rounded - in pixels
            r = Math.atan2(Y, X); //angle in radians (Cartesian system)
        swipeAngle = Math.round(r * 180 / Math.PI); //angle in degrees
        if (swipeAngle < 0) {
            swipeAngle = 360 - Math.abs(swipeAngle);
        }
    }

    function determineSwipeDirection() {
        if ((swipeAngle <= 45) && (swipeAngle >= 0)) {
            swipeDirection = 'left';
        } else if ((swipeAngle <= 360) && (swipeAngle >= 315)) {
            swipeDirection = 'left';
        } else if ((swipeAngle >= 135) && (swipeAngle <= 225)) {
            swipeDirection = 'right';
        } else if ((swipeAngle > 45) && (swipeAngle < 135)) {
            swipeDirection = 'down';
        } else {
            swipeDirection = 'up';
        }
    }

    function processingRoutine() { // actions
        var swipedElement = document.getElementById(triggerElementID);
        if (swipeDirection === 'left') {
            updateText();
            if (carousel) {
                $(carousel).carousel('next'); // move carousel to display next image
            } else {
                pushThumbnails("right", customClass); // move thumbnails
            }
            event.preventDefault(); // cancel normal actions
        } else if (swipeDirection === 'right') {
            updateText();
            if (carousel) {
                $(carousel).carousel('prev'); // move carousel to display previous image
            } else {
                pushThumbnails("left", customClass); // move thumbnails
            }
            event.preventDefault(); // cancel normal touch actions
        }
    }

    function updateText() { //updates the slide details (name, headline) onSlide
        var id = $(customClass + ' .item.active').data('slide-number'); // get the id of the active slide
        $(customClass + ' #carousel-text').html($(customClass + ' #slide-content-' + id).html()); // add corresponding text to headline div
        $(customClass + " #carousel-selector-" + id).addClass("active"); // add active class to active slide
        $(customClass + " .item").each(function (index) {
            if (index !== id) {
                $(customClass + " #carousel-selector-" + index).removeClass("active"); // remove active class for all items that are not active
            }
        });
        $(customClass + ' .image-counter .active-image-number').text(+id + 1); //change current slide number indicator
    }

    function pushThumbnails(direction, customClass) { // shifts the group of thumbnails to the direction specified as a paramter (left or otherwise right)
        var totalGroups = $(customClass + ' .thumbnails').length,
            totalThumbs = 0;
        for (var i = 0; i < totalGroups; i++) {
            totalThumbs += $(customClass + ' .thumbnails#group-' + i + ' > li').length;
        }

        var numberOfThumbnails = 3; // set the number of thumbnails that should be displayed for device width and state
        if ($('.full-screen').length) {
            if ($(window).width() > 615) {
                numberOfThumbnails = 4;
            }
            if ($(window).width() > 1140) {
                numberOfThumbnails = 5;
            }
            if (($(window).width() > 820 && $(window).width() < 979)) {
                numberOfThumbnails = 5;
            }
        } else {
            if ($(window).width() > 979) {
                numberOfThumbnails = 4;
            }
        }
        var numberOfGroups = Math.ceil(totalThumbs / numberOfThumbnails), // number of groups required
            currentSlide = $(customClass + " .thumbnails.active").data('group-number'), // get active slide
            comparitor,
            leftPosition; // new position of group
        if (direction == "left") {
            comparitor = 0; // can't move further left than slide 0
            leftPosition = "+=100%"; // left
        } else {
            comparitor = numberOfGroups - 1; // can't move further right than final slide 
            leftPosition = "-=100%"; // right
        }
        if (currentSlide != comparitor) {
            $(customClass + " .thumbnails").each(function () {
                $(customClass + " #group-" + $(this).data('group-number')).animate({
                    left: leftPosition
                }, "slow"); // move thumbnail group
                $(customClass + " #group-" + $(this).data('group-number')).removeClass("active"); // remove active class
            });
            if (direction == "left") {
                currentSlide -= 1; // decrement active slide variable
            } else {
                currentSlide += 1; // increment active slide variable
            }
            $(customClass + " #group-" + currentSlide).addClass("active"); // add active class
        }
    }
})(jQuery);