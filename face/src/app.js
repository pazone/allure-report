import 'font-awesome/css/font-awesome.css';
import './styles.css';
import './blocks/table/styles.css';
import {Application, Behaviors} from 'backbone.marionette';
import router from './router';
import * as behaviors from './behaviors';
import ErrorLayout from './layouts/error/ErrorLayout';
import i18next, {init as initTranslations} from './util/translation';

Behaviors.behaviorsLookup = behaviors;

function rootPath(path) {
    return path.split('/')[0];
}

function noTabChange() {
    return rootPath(router.getCurrentUrl()) === rootPath(router.lastUrl);
}

export function showView(factory) {
    return (...args) => {
        const current = App.getView();
        if (current && noTabChange()) {
            current.onRouteUpdate(...args);
        } else {
            App.showView(factory(...args));
        }
    };
}

export function notFound() {
    return new ErrorLayout({code: 404, message: 'Not Found'});
}

const App = new Application({
    region: '#content'
});

App.on('start', () => {
    initTranslations();
    i18next.on('languageChanged', () => {
        App.getRegion().reset();
        router.reload();
    });

    router.on('route:notFound', showView(notFound));
});

export default App;
