package mortar;

import android.content.Context;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static mortar.MortarScope.DIVIDER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MortarScopeTest {
  MortarScope.Builder scopeBuilder;

  @Before public void setUp() {
    scopeBuilder = MortarScope.buildRootScope();
  }

  @Test public void illegalScopeName() {
    try {
      scopeBuilder.build("Root" + DIVIDER);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("must not contain");
    }
  }

  @Test public void noServiceRebound() {
    scopeBuilder.withService("ServiceName", new Object());
    try {
      scopeBuilder.withService("ServiceName", new Object());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("cannot be rebound");
    }
  }

  @Test public void nullServiceBound() {
    try {
      scopeBuilder.withService("ServiceName", null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("service == null");
    }
  }

  @Test public void buildScopeWithChild() {
    MortarScope rootScope = scopeBuilder.build("Root");
    MortarScope childScope = rootScope.buildChild().build("Child");
    assertThat(rootScope.children.size()).isEqualTo(1);
    assertThat(childScope.parent).isEqualTo(rootScope);
    assertThat(childScope.getPath()).isEqualTo("Root" + DIVIDER + "Child");
  }

  @Test public void findParentServiceFromChildScope() {
    Object dummyService = new Object();
    MortarScope rootScope = scopeBuilder.withService("ServiceOne", dummyService).build("Root");
    MortarScope childScope = rootScope.buildChild().build("Child");
    assertThat(childScope.getService("ServiceOne")).isEqualTo(dummyService);
  }

  @Test public void noChildrenWithSameName() {
    MortarScope rootScope = scopeBuilder.build("Root");
    rootScope.buildChild().build("childOne");
    try {
      rootScope.buildChild().build("childOne");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageContaining("already has a child named");
    }
  }

  @Test public void throwIfNoServiceFoundForGivenName() {
    Object dummyService = new Object();
    MortarScope rootScope = scopeBuilder.withService("ServiceOne", dummyService).build("Root");
    assertThat(rootScope.getService("ServiceOne")).isNotNull();
    try {
      rootScope.getService("SearchThis");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("No service found named \"SearchThis\"");
    }
  }

  @Test public void throwIfFindChildAfterDestroyed() {
    MortarScope rootScope = scopeBuilder.build("Root");
    MortarScope childScope = rootScope.buildChild().build("ChildOne");

    assertThat(rootScope.findChild("ChildOne")).isNotNull().isEqualTo(childScope);

    rootScope.destroy();
    assertThat(childScope.isDestroyed()).isTrue();
    assertThat(rootScope.isDestroyed()).isTrue();

    try {
      rootScope.findChild("ChildOne");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("destroyed");
    }
  }

  @Test public void throwIfFindServiceAfterDestroyed() {
    Object dummyService = new Object();
    MortarScope rootScope = scopeBuilder.withService("ServiceOne", dummyService).build("Root");
    assertThat(rootScope.getService("ServiceOne")).isEqualTo(dummyService);

    rootScope.destroy();
    assertThat(rootScope.isDestroyed()).isTrue();

    try {
      rootScope.getService("ServiceOne");
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessageContaining("destroyed");
    }
  }

  @Test public void tearDownChildrenBeforeParent() {
    MortarScope rootScope = scopeBuilder.build("Root");
    MortarScope childScope = rootScope.buildChild().build("ChildOne");
    final AtomicBoolean childDestroyed = new AtomicBoolean(false);
    childScope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        childDestroyed.set(true);
      }
    });
    rootScope.register(new Scoped() {
      @Override public void onEnterScope(MortarScope scope) {
      }

      @Override public void onExitScope() {
        assertThat(childDestroyed.get()).isTrue();
      }
    });
    rootScope.destroy();
  }

  @Test public void getScope() {
    MortarScope root = scopeBuilder.build("root");
    Context context = mockContext(root);
    assertThat(MortarScope.getScope(context)).isSameAs(root);
  }

  @Test public void getScopeReturnsDeadScope() {
    MortarScope root = scopeBuilder.build("root");
    Context context = mockContext(root);
    root.destroy();
    assertThat(MortarScope.getScope(context)).isSameAs(root);
  }

  private Context mockContext(MortarScope root) {
    final MortarScope scope = root;
    Context appContext = mock(Context.class);
    when(appContext.getSystemService(anyString())).thenAnswer(new Answer<Object>() {
      @Override public Object answer(InvocationOnMock invocation) throws Throwable {
        String name = (String) invocation.getArguments()[0];
        return scope.hasService(name) ? scope.getService(name) : null;
      }
    });
    return appContext;
  }
}
