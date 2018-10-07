var gulp = require('gulp');
var sass = require('gulp-sass');


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

// this is JS!!!
gulp.task('thirdparty', function () {
    gulp.src('src/main/webapp/static/bootstrap')
       .pipe(sass())
       .pipe(gulp.dest('target/bootstrap'));


    gulp.src('src/main/webapp/static/movie')
        .pipe(sass())
        .pipe(gulp.dest('target/jsmovie'));

    gulp.src('src/main/webapp/static/js')
        .pipe(sass())
        .pipe(gulp.dest('target/jquery'));

});

gulp.task('watch', function() {
    gulp.watch('static/themes/**/*.scss', ['theme']);
});