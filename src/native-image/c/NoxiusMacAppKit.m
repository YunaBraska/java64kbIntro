#import <Cocoa/Cocoa.h>
#import <jni.h>
#include <stdlib.h>

@interface NoxiusAppDelegate : NSObject <NSApplicationDelegate>
@end

static void noxiusExit(void) {
    [NSApp stop:nil];
    exit(0);
}

@implementation NoxiusAppDelegate

- (BOOL)applicationShouldTerminateAfterLastWindowClosed:(NSApplication *)sender {
    (void) sender;
    return YES;
}

- (NSApplicationTerminateReply)applicationShouldTerminate:(NSApplication *)sender {
    (void) sender;
    noxiusExit();
    return NSTerminateNow;
}

- (void)quitNoxius:(id)sender {
    (void) sender;
    noxiusExit();
}

@end

static NoxiusAppDelegate *noxiusDelegate;
static id noxiusKeyMonitor;
static id noxiusWindowCloseObserver;

static void installNoxiusMenu(NSApplication *app) {
    NSMenu *mainMenu = [[NSMenu alloc] initWithTitle:@""];
    NSMenuItem *appMenuItem = [[NSMenuItem alloc] initWithTitle:@""
                                                         action:nil
                                                  keyEquivalent:@""];
    NSMenu *appMenu = [[NSMenu alloc] initWithTitle:@"Noxius64kDemo"];
    NSMenuItem *quitItem = [[NSMenuItem alloc] initWithTitle:@"Quit Noxius64kDemo"
                                                      action:@selector(quitNoxius:)
                                               keyEquivalent:@"q"];
    [quitItem setTarget:noxiusDelegate];
    [appMenu addItem:quitItem];
    [appMenuItem setSubmenu:appMenu];
    [mainMenu addItem:appMenuItem];
    [app setMainMenu:mainMenu];
}

static void installNoxiusQuitHooks(void) {
    if (noxiusKeyMonitor == nil) {
        noxiusKeyMonitor = [NSEvent addLocalMonitorForEventsMatchingMask:NSEventMaskKeyDown
                                                                 handler:^NSEvent *(NSEvent *event) {
            NSString *characters = [event charactersIgnoringModifiers];
            if (([event modifierFlags] & NSEventModifierFlagCommand) != 0
                    && characters != nil
                    && [characters caseInsensitiveCompare:@"q"] == NSOrderedSame) {
                noxiusExit();
            }
            return event;
        }];
    }
    if (noxiusWindowCloseObserver == nil) {
        noxiusWindowCloseObserver = [[NSNotificationCenter defaultCenter] addObserverForName:NSWindowWillCloseNotification
                                                                                      object:nil
                                                                                      queue:nil
                                                                                  usingBlock:^(NSNotification *notification) {
            if ([[notification object] isKindOfClass:[NSWindow class]]
                    && [[(NSWindow *) [notification object] title] isEqualToString:@"Aperture of the Black Sun"]) {
                noxiusExit();
            }
        }];
    }
}

JNIEXPORT void JNICALL Java_berlin_yuna_Noxius64kNativeLauncher_runMacAppLoop(JNIEnv *env, jclass type) {
    (void) env;
    (void) type;

    @autoreleasepool {
        NSApplication *app = [NSApplication sharedApplication];
        noxiusDelegate = [[NoxiusAppDelegate alloc] init];
        [app setDelegate:noxiusDelegate];
        installNoxiusMenu(app);
        installNoxiusQuitHooks();
        [app setActivationPolicy:NSApplicationActivationPolicyRegular];
        [app activateIgnoringOtherApps:YES];
        [app run];
    }
}

JNIEXPORT void JNICALL Java_berlin_yuna_Noxius64kNativeLauncher_stopMacAppLoop(JNIEnv *env, jclass type) {
    (void) env;
    (void) type;

    dispatch_async(dispatch_get_main_queue(), ^{
        [NSApp stop:nil];
        NSEvent *event = [NSEvent otherEventWithType:NSEventTypeApplicationDefined
                                            location:NSZeroPoint
                                       modifierFlags:0
                                           timestamp:0
                                        windowNumber:0
                                             context:nil
                                             subtype:0
                                               data1:0
                                               data2:0];
        [NSApp postEvent:event atStart:NO];
    });
}
