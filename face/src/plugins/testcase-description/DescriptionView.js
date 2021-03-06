import {View} from 'backbone.marionette';
import {className} from '../../decorators';
import template from './DescriptionView.hbs';

@className('pane__section')
class DescriptionView extends View {
    template = template;

    serializeData() {
        return {
            description: this.model.get('description')
        };
    }
}

export default DescriptionView;
