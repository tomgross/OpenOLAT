var gulp = require('gulp');
var sass = require('gulp-sass');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var rename = require('gulp-rename');
var pump = require('pump');
let cleanCSS = require('gulp-clean-css');

var theme = 'light';

gulp.task('theme', function() {

    return gulp.src('static/themes/' + theme + '/theme.scss')
        .pipe(sass({
            includePaths: [
                'static/themes',
                'static/themes/light'
            ],
//            outputStyle: 'compressed'
        }))
        .pipe(gulp.dest('static/themes/' + theme))

});

//script paths
jsTarget = '../../../target/';


gulp.task('compressjs', function (cb) {

    // jquery
    gulp.src('node_modules/jquery/dist/jquery.min.js')
        .pipe(gulp.dest(jsTarget + 'js/jquery'))
        .pipe(rename('jquery-3.3.1.min.js'));

    // plugins: bootstrap, jquery plugins
    gulp.src([
            'static/js/jquery/jquery.periodic.js',
            'static/js/jshashtable-2.1_src.js',
            'static/js/jquery/openolat/jquery.translator.js',
            'static/js/jquery/openolat/jquery.navbar.js',
            'static/js/jquery/openolat/jquery.bgcarrousel.js',
            'static/js/tinymce4/tinymce/jquery.tinymce.min.js',
            'static/functions.js',
            'node_modules/jquery.transit/jquery.transit.js',
            'node_modules/bootstrap/dist/js/bootstrap.min.js'
        ])
        .pipe(concat('js.plugins.min.js'))
        .pipe(uglify())
        .pipe(gulp.dest(jsTarget + 'js'))

    // movie player
    gulp.src('static/movie/player.min.js')
        .pipe(gulp.dest(jsTarget + 'movie'));

    // iframe resizer
    gulp.src('node_modules/iframe-resizer/js/iframeResizer.min.js')
        .pipe(gulp.dest(jsTarget + 'js/iframeResizer'))

});

gulp.task('compresscss', function (cb) {

        gulp.src(
            [
                'static/js/jquery/tagsinput/bootstrap-tagsinput.css',
                'static/js/jquery/fullcalendar/fullcalendar.css',
                'static/js/jquery/cropper/cropper.css',
                'static/js/jquery/sliderpips/jquery-ui-slider-pips.css',
                'static/js/jquery/ui/jquery-ui-1.11.4.custom.min.css',
                'static/js/dragula/dragula.css'
            ])
            .pipe(concat('js.plugins.min.css'))
            .pipe(cleanCSS({debug: true, level: {1: {specialComments: 'none'}}}, (details) => {
                console.log(`${details.name}: ${details.stats.originalSize}`);
                console.log(`${details.name}: ${details.stats.minifiedSize}`);
            }))
            .pipe(gulp.dest(jsTarget + 'jquery'))

})


gulp.task('watch', function() {
    gulp.watch('static/themes/**/*.scss', ['theme']);
});