import SwiftUI
import shared

struct ChecklistItemFormSheet: View {

    @State private var vm: ChecklistItemFormVM

    @Binding private var isPresented: Bool

    init(
            isPresented: Binding<Bool>,
            checklist: ChecklistDb,
            checklistItem: ChecklistItemDb?
    ) {
        _isPresented = isPresented
        vm = ChecklistItemFormVM(checklist: checklist, checklistItem: checklistItem)
    }

    var body: some View {

        VMView(vm: vm, stack: .VStack()) { state in

            HStack {

                Button(
                        action: {
                            isPresented = false
                        },
                        label: { Text("Cancel") }
                )
                        .padding(.leading, 25)

                Spacer()

                Button(
                        action: {
                            vm.save {
                                isPresented = false
                            }
                        },
                        label: {
                            Text("Save")
                                    .fontWeight(.heavy)
                                    .padding(.trailing, 25)
                        }
                )
                        .disabled(!state.isSaveEnabled)
            }
                    .padding(.top, 20)

            MyListView__Padding__HeaderSection()

            MyListView__ItemView(
                    isFirst: true,
                    isLast: true
            ) {

                MyListView__ItemView__TextInputView(
                        text: state.inputNameValue,
                        placeholder: "Name",
                        isAutofocus: true,
                        onValueChanged: { newValue in
                            vm.setInputName(name: newValue)
                        }
                )
            }

            Spacer()
        }
    }
}
