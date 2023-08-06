import behaviorShim from "@/util/behavior-shim";
import Templates from "@/components/dropdowns/templates";
import Utils from "@/components/dropdowns/utils";
import tippy from "tippy.js";

function init() {
  generateButtons();
  generateHandles();
}

function generateHandles() {
  behaviorShim.specify("DIV.dd-handle", "hetero-list", -100, function (e) {
    e.addEventListener("mouseover", function () {
      this.closest(".repeated-chunk").classList.add("hover");
    });
    e.addEventListener("mouseout", function () {
      this.closest(".repeated-chunk").classList.remove("hover");
    });
  });
}

function generateButtons() {
  behaviorShim.specify(
    "DIV.hetero-list-container",
    "hetero-list-new",
    -100,
    function (e) {
      if (isInsideRemovable(e)) {
        return;
      }

      let btn = Array.from(e.querySelectorAll("BUTTON.hetero-list-add")).pop();
      if (!btn) {
        return;
      }
      let prototypes = e.lastElementChild;
      while (!prototypes.classList.contains("prototypes")) {
        prototypes = prototypes.previousElementSibling;
      }
      let insertionPoint = prototypes.previousElementSibling; // this is where the new item is inserted.

      let templates = [];
      let children = prototypes.children;
      for (let i = 0; i < children.length; i++) {
        let n = children[i];
        let name = n.getAttribute("name");
        let tooltip = n.getAttribute("tooltip");
        let descriptorId = n.getAttribute("descriptorId");
        let title = n.getAttribute("title");
        templates.push({
          html: n.innerHTML,
          name: name,
          tooltip: tooltip,
          descriptorId: descriptorId,
          title: title,
        });
      }
      prototypes.remove();
      let withDragDrop = registerSortableDragDrop(e);

      function insert(instance, index) {
        var t = templates[parseInt(index)];
        var nc = document.createElement("div");
        nc.className = "repeated-chunk";
        nc.setAttribute("name", t.name);
        nc.setAttribute("descriptorId", t.descriptorId);
        nc.innerHTML = t.html;
        nc.style.opacity = "0";

        instance.hide();

        renderOnDemand(
          nc.querySelector("div.config-page"),
          function () {
            function findInsertionPoint() {
              // given the element to be inserted 'prospect',
              // and the array of existing items 'current',
              // and preferred ordering function, return the position in the array
              // the prospect should be inserted.
              // (for example 0 if it should be the first item)
              function findBestPosition(prospect, current, order) {
                function desirability(pos) {
                  var count = 0;
                  for (var i = 0; i < current.length; i++) {
                    if (i < pos == order(current[i]) <= order(prospect)) {
                      count++;
                    }
                  }
                  return count;
                }

                var bestScore = -1;
                var bestPos = 0;
                for (var i = 0; i <= current.length; i++) {
                  var d = desirability(i);
                  if (bestScore <= d) {
                    // prefer to insert them toward the end
                    bestScore = d;
                    bestPos = i;
                  }
                }
                return bestPos;
              }

              var current = Array.from(e.children).filter(function (e) {
                return e.matches("DIV.repeated-chunk");
              });

              function o(did) {
                if (did instanceof Element) {
                  did = did.getAttribute("descriptorId");
                }
                for (var i = 0; i < templates.length; i++) {
                  if (templates[i].descriptorId == did) {
                    return i;
                  }
                }
                return 0; // can't happen
              }

              var bestPos = findBestPosition(t.descriptorId, current, o);
              if (bestPos < current.length) {
                return current[bestPos];
              } else {
                return insertionPoint;
              }
            }
            var referenceNode = e.classList.contains("honor-order")
              ? findInsertionPoint()
              : insertionPoint;
            referenceNode.parentNode.insertBefore(nc, referenceNode);

            // Initialize drag & drop for this component
            if (withDragDrop) {
              registerSortableDragDrop(nc);
            }

            new YAHOO.util.Anim(
              nc,
              {
                opacity: { to: 1 },
              },
              0.2,
              YAHOO.util.Easing.easeIn,
            ).animate();

            Behaviour.applySubtree(nc, true);
            ensureVisible(nc);
            layoutUpdateCallback.call();
          },
          true,
        );
      }

      function has(id) {
        return (
          e.querySelector('DIV.repeated-chunk[descriptorId="' + id + '"]') !=
          null
        );
      }

      let oneEach = e.classList.contains("one-each");

      generateDropDown(btn, (instance) => {
        let menuItems = [];
        for (let i = 0; i < templates.length; i++) {
          let n = templates[i];
          let disabled = oneEach && has(n.descriptorId);
          let type = disabled ? "DISABLED" : "button";
          let item = {
            label: n.title,
            onClick: (event) => {
              event.preventDefault();
              event.stopPropagation();
              insert(instance, i);
            },
            type: type,
          };
          menuItems.push(item);
        }
        let menu = Utils.generateDropdownItems(menuItems, true);
        createFilter(menu);
        instance.setContent(menu);
      });
    },
  );
}

function createFilter(menu) {
  const filterInput = document.createElement("input");
  filterInput.classList.add("jenkins-input");
  filterInput.setAttribute("placeholder", "Filter");
  filterInput.setAttribute("spellcheck", "false");
  filterInput.setAttribute("type", "search");

  filterInput.addEventListener("input", (event) =>
    applyFilterKeyword(menu, event.currentTarget),
  );
  filterInput.addEventListener("click", (event) => event.stopPropagation());
  filterInput.addEventListener("keydown", (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
    }
  });

  const filterContainer = document.createElement("div");
  filterContainer.appendChild(filterInput);
  menu.insertBefore(filterContainer, menu.firstChild);
}

function applyFilterKeyword(menu, filterInput) {
  const filterKeyword = (filterInput.value || "").toLowerCase();
  let items = menu.querySelectorAll(
    ".jenkins-dropdown__item, .jenkins-dropdown__disabled",
  );
  for (let item of items) {
    let match = item.innerText.toLowerCase().includes(filterKeyword);
    item.style.display = match ? "inline-flex" : "NONE";
  }
}

function generateDropDown(button, callback) {
  tippy(
    button,
    Object.assign({}, Templates.dropdown(), {
      appendTo: undefined,
      offset: [0, 5],
      onCreate(instance) {
        if (instance.loaded) {
          return;
        }
        instance.popper.addEventListener("click", () => {
          instance.hide();
        });
        instance.popper.addEventListener("keydown", () => {
          if (event.key === "Escape") {
            instance.hide();
          }
        });
      },
      onShow(instance) {
        callback(instance);
        button.dataset.expanded = "true";
      },
      onHide() {
        button.dataset.expanded = "false";
      },
    }),
  );
}

export default { init };
