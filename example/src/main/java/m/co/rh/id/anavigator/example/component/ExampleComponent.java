package m.co.rh.id.anavigator.example.component;

import androidx.annotation.NonNull;

public class ExampleComponent implements IExampleComponent {
    private String mName;

    public ExampleComponent(String name) {
        mName = name;
    }

    @NonNull
    @Override
    public String toString() {
        return mName;
    }
}
