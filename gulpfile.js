var gulp = require('gulp');
var sass = require('gulp-sass');
var uglify = require('gulp-uglify');
var concat = require('gulp-concat');
var rename = require('gulp-rename');
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
assetsPath = 'src/main/webapp/';


gulp.task('compressjs', function () {

    // jquery
    gulp.src('node_modules/jquery/dist/jquery.min.js')
        .pipe(gulp.dest(assetsPath + 'js/jquery'))
        .pipe(rename('jquery-3.3.1.min.js'));

    // plugins: bootstrap, jquery plugins
    gulp.src([
            assetsPath + 'static/js/jquery/jquery.periodic.js',
            assetsPath + 'static/js/jshashtable-2.1_src.js',
            assetsPath + 'static/js/jquery/openolat/jquery.translator.js',
            assetsPath + 'static/js/jquery/openolat/jquery.navbar.js',
            assetsPath + 'static/js/jquery/openolat/jquery.bgcarrousel.js',
            assetsPath + 'static/js/tinymce4/tinymce/jquery.tinymce.min.js',
            assetsPath + 'static/functions.js',
            assetsPath + 'node_modules/jquery.transit/jquery.transit.js',
            assetsPath + 'node_modules/bootstrap/dist/js/bootstrap.min.js'
        ])
        .pipe(concat('js.plugins.min.js'))
        .pipe(uglify())
        .pipe(gulp.dest(assetsPath + 'js'))

    // movie player
    gulp.src('static/movie/player.min.js')
        .pipe(gulp.dest(assetsPath + 'movie'));

    // iframe resizer
    gulp.src('node_modules/iframe-resizer/js/iframeResizer.min.js')
        .pipe(gulp.dest(assetsPath + 'js/iframeResizer'))

});

gulp.task('compresscss', function () {

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
            .pipe(gulp.dest(assetsPath + 'jquery'))

})


gulp.task('watch', function() {
    gulp.watch('static/themes/**/*.scss', ['theme']);
});

// Default Task
gulp.task('default', ['compresscss', 'compressjs']);