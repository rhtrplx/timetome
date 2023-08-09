import SwiftUI
import UniformTypeIdentifiers
import MessageUI
import shared

struct SettingsSheet: View {

    @Binding var isPresented: Bool

    @State private var vm = SettingsSheetVM()

    @State private var isReadmePresented = false

    @State private var mailViewResult: Result<MFMailComposeResult, Error>? = nil
    @State private var isMailViewPresented = false

    @State private var isFileImporterPresented = false

    @State private var isFoldersSettingsPresented = false
    @State private var isDayStartPresented = false

    @State private var isFileExporterPresented = false
    @State private var fileForExport: MyJsonFileDocument? = nil
    @State private var fileForExportName: String? = nil

    @State private var isAddChecklistPresented = false

    @State private var isAddShortcutPresented = false

    @State private var sheetHeaderScroll = 0

    //////

    var body: some View {

        VMView(vm: vm, stack: .VStack()) { state in

            SheetHeaderView(
                    onCancel: { isPresented.toggle() },
                    title: state.headerTitle,
                    doneText: nil,
                    isDoneEnabled: false,
                    scrollToHeader: sheetHeaderScroll,
                    cancelText: "Back"
            ) {}

            ScrollViewWithVListener(showsIndicators: false, vScroll: $sheetHeaderScroll) {

                VStack {

                    ///
                    /// Checklists

                    VStack {

                        MyListView__Padding__SectionHeader()

                        MyListView__HeaderView(
                                title: "CHECKLISTS",
                                rightView: AnyView(
                                        Button(
                                                action: {
                                                    isAddChecklistPresented.toggle()
                                                },
                                                label: {
                                                    Image(systemName: "plus")
                                                }
                                        )
                                )
                        )

                        MyListView__Padding__HeaderSection()

                        let checklists = state.checklists
                        ForEach(checklists, id: \.id) { checklist in
                            let isFirst = checklists.first == checklist
                            MyListView__ItemView(
                                    isFirst: isFirst,
                                    isLast: checklists.last == checklist,
                                    withTopDivider: !isFirst
                            ) {
                                ToolsView_ChecklistView(checklist: checklist)
                            }
                        }
                    }

                    ///
                    /// Shortcuts

                    VStack {

                        MyListView__Padding__SectionHeader()

                        MyListView__HeaderView(
                                title: "SHORTCUTS",
                                rightView: AnyView(
                                        Button(
                                                action: {
                                                    isAddShortcutPresented.toggle()
                                                },
                                                label: {
                                                    Image(systemName: "plus")
                                                }
                                        )
                                )
                        )

                        MyListView__Padding__HeaderSection()

                        let shortcuts = state.shortcuts
                        ForEach(shortcuts, id: \.id) { shortcut in
                            let isFirst = shortcuts.first == shortcut
                            MyListView__ItemView(
                                    isFirst: isFirst,
                                    isLast: shortcuts.last == shortcut,
                                    withTopDivider: !isFirst
                            ) {
                                ToolsView_ShortcutView(shortcut: shortcut)
                            }
                        }
                    }

                    ///
                    /// Settings

                    VStack {

                        MyListView__Padding__SectionHeader()

                        MyListView__HeaderView(title: "SETTINGS")

                        MyListView__Padding__HeaderSection()

                        MyListView__ItemView(
                                isFirst: true,
                                isLast: false
                        ) {
                            MyListView__ItemView__ButtonView(
                                    text: "Folders",
                                    withArrow: true
                            ) {
                                isFoldersSettingsPresented = true
                            }
                                    .sheetEnv(isPresented: $isFoldersSettingsPresented) {
                                        FoldersSettingsSheet(isPresented: $isFoldersSettingsPresented)
                                    }
                        }

                        MyListView__ItemView(
                                isFirst: false,
                                isLast: true,
                                withTopDivider: true
                        ) {
                            MyListView__ItemView__ButtonView(
                                    text: "Day Start",
                                    rightView: AnyView(
                                            MyListView__ItemView__ButtonView__RightText(
                                                    text: state.dayStartNote
                                            )
                                    )
                            ) {
                                isDayStartPresented = true
                            }
                                    .sheetEnv(isPresented: $isDayStartPresented) {
                                        DayStartDialog(
                                                isPresented: $isDayStartPresented,
                                                settingsSheetVM: vm,
                                                settingsSheetState: state
                                        )
                                    }
                        }
                    }

                    ///
                    /// Backup

                    VStack {

                        MyListView__Padding__SectionHeader()

                        MyListView__HeaderView(title: "BACKUPS")

                        MyListView__Padding__HeaderSection()

                        MyListView__ItemView(
                                isFirst: true,
                                isLast: false
                        ) {

                            MyListView__ItemView__ButtonView(text: "Create") {
                                Task {
                                    let jString = try await Backup.shared.create(type: "manual", intervalsLimit: 999_999_999.toInt32()) // todo
                                    fileForExportName = vm.prepBackupFileName()
                                    fileForExport = MyJsonFileDocument(initialText: jString)
                                    isFileExporterPresented = true
                                }
                            }
                        }

                        MyListView__ItemView(
                                isFirst: false,
                                isLast: false,
                                withTopDivider: true
                        ) {

                            MyListView__ItemView__ButtonView(text: "Restore") {
                                isFileImporterPresented = true
                            }
                        }

                        MyListView__ItemView(
                                isFirst: false,
                                isLast: true,
                                withTopDivider: true
                        ) {

                            MyListView__ItemView__ButtonView(
                                    text: "Auto Backup",
                                    rightView: AnyView(
                                            MyListView__ItemView__ButtonView__RightText(
                                                    text: state.autoBackupTimeString
                                            )
                                    )
                            ) {
                                // todo do catch
                                // https://stackoverflow.com/a/64592118/5169420
                                let path = try! AutoBackupIos.autoBackupsFolder()
                                        .absoluteString
                                        .replacingOccurrences(of: "file://", with: "shareddocuments://")
                                UIApplication.shared.open(URL(string: path)!)
                            }
                        }
                    }


                    ///
                    /// Mics

                    MyListView__Padding__SectionHeader()

                    MyListView__ItemView(
                            isFirst: true,
                            isLast: false,
                            withTopDivider: false
                    ) {
                        MyListView__ItemView__ButtonView(text: "How to Use") {
                            isReadmePresented.toggle()
                        }
                    }
                            .sheetEnv(isPresented: $isReadmePresented) {
                                ReadmeSheet(isPresented: $isReadmePresented)
                            }

                    MyListView__ItemView(
                            isFirst: false,
                            isLast: false,
                            withTopDivider: true
                    ) {
                        MyListView__ItemView__ButtonView(text: "Ask a Question") {
                            if (MFMailComposeViewController.canSendMail()) {
                                isMailViewPresented.toggle()
                            } else {
                                // Взято из skorolek
                                let subjectEncoded = state.feedbackSubject.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
                                let url = URL(string: "mailto:\(state.feedbackEmail)?subject=\(subjectEncoded)")!
                                UIApplication.shared.open(url)
                            }
                        }
                                .sheetEnv(isPresented: $isMailViewPresented) {
                                    MailView(
                                            toEmail: state.feedbackEmail,
                                            subject: state.feedbackSubject,
                                            body: nil,
                                            result: $mailViewResult
                                    )
                                }
                    }

                    MyListView__ItemView(
                            isFirst: false,
                            isLast: true,
                            withTopDivider: true
                    ) {

                        MyListView__ItemView__ButtonView(text: "Open Source") {
                            UIApplication.shared.open(URL(string: state.openSourceUrl)!)
                        }
                    }
                }

                HStack {
                    Text("timeto.me for iOS v" + state.appVersion)
                            .foregroundColor(.secondary)
                            .font(.system(size: 16))
                }
                        .padding(.top, 24)
                        .padding(.bottom, 34)
            }
        }
                .background(c.sheetBg)
                .sheetEnv(isPresented: $isAddChecklistPresented) {
                    ChecklistFormSheet(isPresented: $isAddChecklistPresented, checklist: nil)
                }
                .sheetEnv(isPresented: $isAddShortcutPresented) {
                    ShortcutFormSheet(isPresented: $isAddShortcutPresented, editedShortcut: nil)
                }
                .onAppear {
                    // todo auto backups https://www.hackingwithswift.com/books/ios-swiftui/writing-data-to-the-documents-directory
                    // let str = "Test Message"
                    // let dd = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
                    // let url = dd.appendingPathComponent("message.txt")
                    // do {
                    // try str.write(to: url, atomically: true, encoding: .utf8)
                    // let input = try String(contentsOf: url)
                    // print(url)
                    // } catch {
                    // print(error.localizedDescription)
                    // }
                }
                .fileExporter(
                        isPresented: $isFileExporterPresented,
                        document: fileForExport,
                        // I do not know why, but I have to set contentType.
                        // It also set in MyJsonFileDocument.
                        contentType: .json,
                        defaultFilename: fileForExportName
                ) { result in
                    switch result {
                    case .success(let url):
                        // todo
                        print("Saved to \(url.absoluteString.removingPercentEncoding!)")
                    case .failure(let error):
                        // todo
                        print(error.myMessage())
                    }
                }
                .fileImporter(isPresented: $isFileImporterPresented, allowedContentTypes: [.json]) { res in
                    do {
                        let fileUrl = try res.get()

                        ///
                        /// https://stackoverflow.com/a/64351217
                        if !fileUrl.startAccessingSecurityScopedResource() {
                            throw MyError("iOS restore !fileUrl.startAccessingSecurityScopedResource()")
                        }
                        guard let jString = String(data: try Data(contentsOf: fileUrl), encoding: .utf8) else {
                            throw MyError("iOS restore jString null")
                        }
                        fileUrl.stopAccessingSecurityScopedResource()
                        //////

                        try Backup.shared.restore(jString: jString)

                        isPresented = false
                    } catch {
                        UtilsKt.showUiAlert(message: "Error", reportApiText: "iOS restore exception\n" + error.myMessage())
                    }
                }
    }

    /// https://www.hackingwithswift.com/quick-start/swiftui/how-to-export-files-using-fileexporter
    private struct MyJsonFileDocument: FileDocument {

        // Otherwise .txt file
        static var readableContentTypes = [UTType.json]

        var text = ""

        init(initialText: String = "") {
            text = initialText
        }

        init(configuration: ReadConfiguration) throws {
            if let data = configuration.file.regularFileContents {
                text = String(decoding: data, as: UTF8.self)
            }
        }

        func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
            let data = Data(text.utf8)
            return FileWrapper(regularFileWithContents: data)
        }
    }

    private struct DayStartDialog: View {

        @Binding private var isPresented: Bool
        @State private var selectedDayStart: Int32 // WARNING Int32!
        private let settingsSheetVM: SettingsSheetVM
        private let settingsSheetState: SettingsSheetVM.State

        init(
                isPresented: Binding<Bool>,
                settingsSheetVM: SettingsSheetVM,
                settingsSheetState: SettingsSheetVM.State
        ) {
            _isPresented = isPresented
            self.settingsSheetVM = settingsSheetVM
            self.settingsSheetState = settingsSheetState
            _selectedDayStart = State(initialValue: settingsSheetState.dayStartSeconds)
        }

        var body: some View {

            VStack {

                HStack {

                    Button(
                            action: { isPresented.toggle() },
                            label: { Text("Cancel") }
                    )
                            .padding(.leading, 25)

                    Spacer()

                    Button(
                            action: {
                                settingsSheetVM.upDayStartOffsetSeconds(seconds: selectedDayStart) {
                                    isPresented = false
                                }
                            },
                            label: {
                                Text("Save")
                                        .fontWeight(.heavy)
                                        .padding(.trailing, 25)
                            }
                    )
                }
                        .padding(.top, 20)

                Spacer()

                Picker(
                        "",
                        selection: $selectedDayStart
                ) {
                    ForEach(settingsSheetState.dayStartListItems, id: \.seconds) { item in
                        Text(item.note).tag(item.seconds)
                    }
                }
                        .pickerStyle(WheelPickerStyle())

                Spacer()
            }
        }
    }
}

struct ToolsView_ChecklistView: View {

    let checklist: ChecklistModel

    @State private var isItemsPresented = false
    @State private var isEditPresented = false

    var body: some View {
        MyListSwipeToActionItem(
                deletionHint: checklist.name,
                deletionConfirmationNote: "Are you sure you want to delete \"\(checklist.name)\" checklist?",
                onEdit: {
                    isEditPresented = true
                },
                onDelete: {
                    checklist.deleteWithDependencies { _ in
                        // todo
                    }
                }
        ) {
            AnyView(safeView)
                    .padding(.horizontal, MyListView.PADDING_INNER_HORIZONTAL)
                    .padding(.vertical, DEF_LIST_V_PADDING)
        }
                .sheetEnv(isPresented: $isEditPresented) {
                    ChecklistFormSheet(isPresented: $isEditPresented, checklist: checklist)
                }
    }

    private var safeView: some View {
        Button(
                action: {
                    isItemsPresented = true
                },
                label: {
                    HStack {
                        Text(checklist.name)
                        Spacer()
                    }
                }
        )
                .foregroundColor(.primary)
                .sheetEnv(isPresented: $isItemsPresented) {
                    ChecklistDialog(isPresented: $isItemsPresented, checklist: checklist)
                }
    }
}

struct ToolsView_ShortcutView: View {

    let shortcut: ShortcutModel

    @State private var isEditPresented = false

    var body: some View {
        MyListSwipeToActionItem(
                deletionHint: shortcut.name,
                deletionConfirmationNote: "Are you sure you want to delete \"\(shortcut.name)\" shortcut?",
                onEdit: {
                    isEditPresented = true
                },
                onDelete: {
                    shortcut.delete { err in
                        // todo report
                    }
                }
        ) {
            AnyView(itemView)
                    .padding(.horizontal, MyListView.PADDING_INNER_HORIZONTAL)
                    .padding(.vertical, DEF_LIST_V_PADDING)
        }
                .sheetEnv(isPresented: $isEditPresented) {
                    ShortcutFormSheet(isPresented: $isEditPresented, editedShortcut: shortcut)
                }
    }

    private var itemView: some View {
        Button(
                action: {
                    shortcut.performUI()
                },
                label: {
                    HStack {
                        Text(shortcut.name)
                        Spacer()
                    }
                }
        )
                .foregroundColor(.primary)
    }
}
